def psql(ctx):
    return """
set -eu

PG_AUTH_HOST="db-rw-service"
PG_DATABASE="logos"
PG_DB_MIGRATION_USER="root"

if [ -f /var/run/secrets/eks.amazonaws.com/serviceaccount/token ]
then
    PG_PORT=5432
    PG_AUTH_RESOLVED_HOST="$(nslookup -type=cname "$PG_AUTH_HOST" | grep "canonical name = " | cut -d' ' -f4 | sed -e 's/\\.$//')"
    PG_HOST="$PG_AUTH_RESOLVED_HOST"
else
    PG_HOST="127.0.0.1"
    PG_PORT=15432
    CONSOLE_POD_NAME="pod/$({kubectl} get pods -l app=console -o jsonpath="{{.items[0].metadata.name}}")"
    export KUBECONFIG=$HOME/.kube/config
    {kubectl} wait --for=condition=Ready "$CONSOLE_POD_NAME"

    PG_AUTH_RESOLVED_HOST="$({kubectl} exec "$CONSOLE_POD_NAME" -- nslookup -type=cname "$PG_AUTH_HOST" | grep "canonical name = " | cut -d' ' -f4 | sed -e 's/\\.$//')"
fi

export PGPASSWORD="$(aws rds generate-db-auth-token \
                             --hostname "$PG_AUTH_RESOLVED_HOST" \
                             --port "5432" \
                             --region "$LOGOS_AWS_REGION" \
                             --username "$PG_DB_MIGRATION_USER")"

psql() {{
    /usr/bin/psql \
         --host "$PG_HOST" \
         --port "$PG_PORT" \
         -U "$PG_DB_MIGRATION_USER" \
         -v ON_ERROR_STOP=1 \
         --no-psqlrc \
         "$PG_DATABASE" \
         "$@"
}}
"""

def _pg_migrate_impl(ctx):
    result = ctx.actions.declare_file(ctx.label.name + "_migrations.txt")
    ctx.actions.run_shell(
        command = (psql(ctx) + """
for migration in {migrations}
do
    psql -f "$migration"
    [[ "$(basename "$migration")" == "{head}" ]] && break
done

psql -A -t -c "select name from migrations.migration order by id" > $1
""").format(
            kubectl = ctx.files.kubectl[0].path,
            migrations = " ".join([f.path for f in ctx.files.migrations]),
            head = ctx.attr.head,
        ),
        outputs = [result],
        use_default_shell_env = True,
        arguments = [result.path],
    )
    return [
        DefaultInfo(files = depset([result])),
    ]

pg_migrate_rule = rule(
    implementation = _pg_migrate_impl,
    attrs = {
        "migrations": attr.label_list(mandatory = True, allow_files = True),
        "head": attr.string(mandatory = True),
        "deps": attr.label_list(),
        "kubectl": attr.label(
            cfg = "exec",
            default = Label("@logos//tools:kubectl"),
            executable = True,
        ),
    },
)

def _pg_check_impl(ctx):
    result = ctx.actions.declare_file(ctx.label.name + "_pg_check.txt")
    ctx.actions.run_shell(
        command = (psql(ctx) + """
psql -A -t -c "select name from migrations.migration order by id" > $1
""").format(
            kubectl = ctx.attr.kubectl.files_to_run.executable.path,
        ),
        outputs = [result],
        arguments = [result.path],
    )
    return [
        DefaultInfo(files = depset([result])),
    ]

pg_check_rule = rule(
    implementation = _pg_check_impl,
    attrs = {
        "kubectl": attr.label(
            cfg = "exec",
            default = Label("@logos//tools:kubectl"),
            executable = True,
        ),
    },
)

def pg_migrate(name, migrations, head = None, deps = None, visibility = None):
    pg_migrate_rule(
        name = name,
        migrations = migrations,
        head = head,
        deps = deps,
        tags = [
            "external",
            "no-remote",
            "no-sandbox",
            "requires-network",
        ],
        visibility = visibility,
    )
