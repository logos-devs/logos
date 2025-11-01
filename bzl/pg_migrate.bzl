load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")

def _aws_enabled_flag(ctx):
    infra_provider = ctx.attr._infra[BuildSettingInfo].value
    return "true" if infra_provider.lower() == "aws" else "false"


def psql(ctx, aws_enabled_flag):
    aws_region = ctx.attr._aws_region[BuildSettingInfo].value
    kubectl_context = ctx.attr._kubectl_context[BuildSettingInfo].value
    script = """
set -eu

PG_AUTH_HOST="db-rw-service"
PG_DATABASE="logos"
PG_DB_MIGRATION_USER="root"
AWS_REGION="__AWS_REGION__"
KUBECTL="__KUBECTL_PATH__"
KUBECTL_CONTEXT="__KUBECTL_CONTEXT__"
if [ -n "$KUBECTL_CONTEXT" ]; then
    KUBECTL="$KUBECTL --context=$KUBECTL_CONTEXT"
fi

if [ -f /var/run/secrets/eks.amazonaws.com/serviceaccount/token ]
then
    PG_PORT=5432
    PG_AUTH_RESOLVED_HOST="$(nslookup -type=cname "$PG_AUTH_HOST" | grep "canonical name = " | cut -d' ' -f4 | sed -e 's/\\.$//')"
    PG_HOST="$PG_AUTH_RESOLVED_HOST"
else
    PG_HOST="127.0.0.1"
    PG_PORT=15432
    CONSOLE_POD_NAME="pod/$($KUBECTL get pods -l app=console -o jsonpath="{{.items[0].metadata.name}}")"
    export KUBECONFIG=$HOME/.kube/config
    $KUBECTL wait --for=condition=Ready "$CONSOLE_POD_NAME"

    PG_AUTH_RESOLVED_HOST="$($KUBECTL exec "$CONSOLE_POD_NAME" -- nslookup -type=cname "$PG_AUTH_HOST" | grep "canonical name = " | cut -d' ' -f4 | sed -e 's/\\.$//')"
fi

AWS_ENABLED="__AWS_ENABLED__"

if [ "$AWS_ENABLED" = "false" ]; then
    if [ -z "${STORAGE_PG_BACKEND_PASSWORD:-}" ]; then
        echo "STORAGE_PG_BACKEND_PASSWORD must be set when AWS infrastructure is disabled" >&2
        exit 1
    fi
    export PGPASSWORD="$STORAGE_PG_BACKEND_PASSWORD"
    else
        export PGPASSWORD="$(aws rds generate-db-auth-token \
                                 --hostname "$PG_AUTH_RESOLVED_HOST" \
                                 --port "5432" \
                                 --region "$AWS_REGION" \
                                 --username "$PG_DB_MIGRATION_USER")"
    fi

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
    return (
        script
        .replace("__AWS_ENABLED__", aws_enabled_flag)
        .replace("__AWS_REGION__", aws_region)
        .replace("__KUBECTL_CONTEXT__", kubectl_context)
        .replace("__KUBECTL_PATH__", ctx.attr.kubectl.files_to_run.executable.short_path)
    )


def _pg_migrate_impl(ctx):
    aws_enabled_flag = _aws_enabled_flag(ctx)
    result = ctx.actions.declare_file(ctx.label.name + "_migrations.txt")
    ctx.actions.run_shell(
        command = (psql(ctx, aws_enabled_flag) + """
for migration in {migrations}
do
    psql -f "$migration"
    [[ "$(basename "$migration")" == "{head}" ]] && break
done

psql -A -t -c "select name from migrations.migration order by id" > $1
""").format(
            kubectl = ctx.attr.kubectl.files_to_run.executable.short_path,
            migrations = " ".join([f.path for f in ctx.files.migrations]),
            head = ctx.attr.head,
        ),
        outputs = [result],
        tools = [ctx.attr.kubectl],
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
        "_infra": attr.label(default = Label("//dev/logos/config/infra:provider")),
        "_aws_region": attr.label(default = Label("//dev/logos/stack/aws:region")),
        "_kubectl_context": attr.label(default = Label("//dev/logos/config/kubectl:context")),
    },
)


def _pg_check_impl(ctx):
    aws_enabled_flag = _aws_enabled_flag(ctx)
    result = ctx.actions.declare_file(ctx.label.name + "_pg_check.txt")
    ctx.actions.run_shell(
        command = (psql(ctx, aws_enabled_flag) + """
psql -A -t -c "select name from migrations.migration order by id" > $1
""").format(
            kubectl = ctx.attr.kubectl.files_to_run.executable.short_path,
        ),
        outputs = [result],
        tools = [ctx.attr.kubectl],
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
        "_infra": attr.label(default = Label("//dev/logos/config/infra:provider")),
        "_aws_region": attr.label(default = Label("//dev/logos/stack/aws:region")),
        "_kubectl_context": attr.label(default = Label("//dev/logos/config/kubectl:context")),
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
