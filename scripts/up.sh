#!/bin/bash -eu

WORKSPACE_DIR="$(dirname "$(realpath "$0")")/.."
REGION=us-east-2

PG_TUNNEL_HOST="127.0.0.1"
PG_TUNNEL_PORT=15432
PG_AUTH_HOST="db-rw.logos.dev"
PG_AUTH_PORT=5432
PG_DB_NAME="logos"
PG_DB_MIGRATION_USER="root"

trap 'cleanup' ERR

cleanup() {
    if [[ -n ${TUNNEL_PID:-} ]] && kill -0 "$TUNNEL_PID"
    then
        echo "Killing PID: $TUNNEL_PID"
        kill "$TUNNEL_PID"
    fi
}

die() {
    echo "$@" 2>&1
    exit 1
}

command -v jq || die "Please install jq."

BAZEL="bazelisk"
ACCOUNT="$(aws sts get-caller-identity --query "Account" --output text)"
REGION="$(aws configure get region)"
STACK="logos-eks"

export AWS_DEFAULT_REGION="$REGION"
$BAZEL run //dev/logos/infra:cdk -- deploy --all --no-rollback

ROLE_ARN="$(aws cloudformation describe-stacks \
                    --stack-name $STACK \
                    --query "Stacks[0].Outputs[?starts_with(OutputKey, \`logoseksConfigCommand\`)].OutputValue | [0]" \
                    --output text \
                    | cut -d' ' -f 9)"

echo "Updating kubeconfig for $STACK in $REGION"
aws eks update-kubeconfig \
            --name logos-eks \
            --region "$REGION" \
            --role-arn "$ROLE_ARN"

echo "Logging into ECR for $ACCOUNT in $REGION"
aws ecr get-login-password \
            --region "$REGION" \
            | docker login \
                --username AWS \
                --password-stdin "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"

$BAZEL run //dev/logos/stack/service/console:console.apply

CONSOLE_POD_NAME="pod/$(kubectl get pods -l app=console -o jsonpath="{.items[0].metadata.name}")"
kubectl wait --for=condition=Ready "$CONSOLE_POD_NAME"
CLUSTER_RW_ENDPOINT="$(kubectl exec "$CONSOLE_POD_NAME" -- dig +short cname "$PG_AUTH_HOST" | sed -e 's/.$//')"

"$WORKSPACE_DIR"/scripts/db_tunnel.sh &
TUNNEL_PID=$!
echo "Started database tunnel at PID: $TUNNEL_PID"
sleep 3


_psql() {
    psql --host "$PG_TUNNEL_HOST" --port "$PG_TUNNEL_PORT" "$@"
}

rds_auth_token() {
    USERNAME=$1; shift 1

    aws rds generate-db-auth-token \
              --hostname "$CLUSTER_RW_ENDPOINT" \
              --port "$PG_AUTH_PORT" \
              --region "$REGION" \
              --username "$USERNAME"
}

PGPASSWORD="$(aws secretsmanager \
                  get-secret-value \
                  --secret-id "$(aws secretsmanager list-secrets \
                                                    --query "SecretList[?starts_with(Name, \`logosrdsdbclusterSecret\`)].Name | [0]" \
                                                    --output text)" \
                  --query "SecretString" \
                  --output text | jq -r ".password")"
export PGPASSWORD

_psql -U clusteradmin template1 <<SQL
    do language plpgsql \$\$
        begin
            if not exists (
                select from pg_roles where rolname = '$PG_DB_MIGRATION_USER'
            ) then
                execute format('create role $PG_DB_MIGRATION_USER');
            end if;
        end;
    \$\$;

    alter database logos owner to $PG_DB_MIGRATION_USER;

    alter role $PG_DB_MIGRATION_USER login;
    grant rds_iam to $PG_DB_MIGRATION_USER;
    grant rds_superuser to $PG_DB_MIGRATION_USER;
SQL

unset PGPASSWORD

SQITCH_PASSWORD="$(rds_auth_token "$PG_DB_MIGRATION_USER")"
export SQITCH_PASSWORD

pushd "$WORKSPACE_DIR/dev/logos/stack/service/storage/migrations"
sqitch deploy --db-name "$PG_DB_NAME" \
              --db-user "$PG_DB_MIGRATION_USER" \
              --db-host "$PG_TUNNEL_HOST" \
              --db-port "$PG_TUNNEL_PORT"
popd

unset SQITCH_PASSWORD

export STORAGE_PG_BACKEND_JDBC_URL="jdbc:postgresql://localhost:15432/logos"
export STORAGE_PG_BACKEND_USER="storage"
export STORAGE_PG_BACKEND_HOST="$CLUSTER_RW_ENDPOINT"

$BAZEL run //dev/logos/stack/service/backend:backend.apply
$BAZEL run //dev/logos/stack/service/envoy:envoy.apply
