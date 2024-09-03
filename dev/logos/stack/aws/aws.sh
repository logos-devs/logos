#!/bin/bash -eu

SELF="$0"
cd "$BUILD_WORKSPACE_DIRECTORY"

AWS_REGION="${AWS_REGION:-"us-east-2"}"
# TODO create a role with diminished permissions
AWS_SSO_ROLE="${AWS_SSO_ROLE:-"AWSAdministratorAccess"}"
AWS_SSO_URL="${AWS_SSO_URL:-"https://logos-dev.awsapps.com/start"}"
BAZEL="bazel"
SED="$(command -v gsed || command -v sed)"

PG_TUNNEL_HOST="127.0.0.1"
PG_TUNNEL_PORT=15432
PG_TUNNEL_SOCAT_PID="/tmp/logos_rds_tunnel_socat_pid"
PG_TUNNEL_PORT_FORWARD_PID="/tmp/logos_rds_tunnel_port_forward_pid"
PG_TUNNEL_LOCAL_PORT=15432
PG_TUNNEL_REMOTE_PORT=5432
PG_AUTH_PORT=5432
PG_AUTH_HOST="db-rw-service"
PG_AUTH_RESOLVED_HOST=""
PG_DB_NAME="logos"
PG_DB_MIGRATION_USER="root"
SERVER_TUNNEL_PID="/tmp/logos_server_tunnel_pid"

trap 'cleanup' INT TERM ERR

_update_bazelrc_env() {
  local var_name="$1"; shift 1
  local var_value="$1"; shift 1
  local bazelrc_local="$BUILD_WORKSPACE_DIRECTORY/.bazelrc.local"

  touch "$bazelrc_local"
  echo -e "Updating $bazelrc_local with env var $var_name=$var_value"

  "$SED" -i "/action_env=$var_name=/d" "$bazelrc_local" || true
  echo "build --action_env=$var_name=$var_value" >> "$bazelrc_local"
}

_run_with_pid() {
    PID_FILE="$1"; shift 1

    if ! [ -f "$PID_FILE" ] || ! kill -0 "$(cat "$PID_FILE")"; then
        "$@" &
        echo "$!" > "$PID_FILE"
    fi
}

_kill_with_pid() {
    PID_FILE="$1"; shift 1
    MSG="$1"; shift 1

    if [ -f "$PID_FILE" ]; then
        PID="$(cat "$PID_FILE")"
        if kill -0 "$PID" 2> /dev/null; then
            stderr
            stderr "$MSG"
            (kill -SIGINT "$PID" && rm "$PID_FILE") || true
        fi
        rm -f "$PID_FILE"
    fi
}

cleanup() {
    trap - INT TERM ERR
}

stderr() {
  echo -e "$@" 2>&1
}

dev_config_aws_profile() {
  AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-""}"

  while [ -z "$AWS_ACCOUNT_ID" ]; do
    read -r -p "AWS Account ID: " AWS_ACCOUNT_ID
  done
  stderr

  cat <<EOF
[default]
sso_account_id = $AWS_ACCOUNT_ID
sso_role_name = $AWS_SSO_ROLE
region = $AWS_REGION
sso_start_url = $AWS_SSO_URL
sso_region = $AWS_REGION
sso_registration_scopes = sso:account:access
EOF
}

await_console_pod() {
    CONSOLE_POD_NAME="pod/$(kubectl get pods -l app=console -o jsonpath="{.items[0].metadata.name}")"
    kubectl wait --for=condition=Ready "$CONSOLE_POD_NAME"
}

dev_env() {
    if [ -z "$PG_AUTH_RESOLVED_HOST" ]
    then
        await_console_pod
        PG_AUTH_RESOLVED_HOST="$(kubectl exec "$CONSOLE_POD_NAME" -- nslookup -type=cname "$PG_AUTH_HOST" | grep "canonical name = " | cut -d' ' -f4 | sed -e 's/\.$//')"
    fi
}

dev_server() {
  dev_env
  telepresence intercept backend-deployment --port 8081 -- ibazel run //dev/logos/service/backend/server
}

dev_client() {
  dev_env
  telepresence intercept client-deployment --port 8080 -- ibazel run //dev/logos/service/client/web:dev
}

dev_setup() {
    mkdir -p ~/.aws
    dev_config_aws_profile > ~/.aws/config

    aws sso login --no-browser

    bazel run @logos//dev/logos/stack/aws/cdk -- deploy --all --require-approval never

    ACCOUNT="$(aws sts get-caller-identity --query "Account" --output text)"
    STACK="logos-eks-stack"

    echo "Updating kubeconfig for $STACK in $AWS_REGION"
    aws eks update-kubeconfig \
                --name logos-eks-stack-cluster \
                --region "$AWS_REGION"

    # pg_migrate bzl rule needs the absolute path to aws cli
    sed -i 's|command: aws|command: /usr/local/bin/aws|' ~/.kube/config

    EKS_STACK_CLUSTER="logos-eks-stack-cluster"

    _update_bazelrc_env LOGOS_AWS_ACCOUNT_ID "$AWS_ACCOUNT_ID"
    _update_bazelrc_env LOGOS_AWS_REGION "$AWS_REGION"
    _update_bazelrc_env LOGOS_AWS_EKS_CLUSTER "arn:aws:eks:$AWS_REGION:$AWS_ACCOUNT_ID:cluster/$EKS_STACK_CLUSTER"
    _update_bazelrc_env LOGOS_AWS_EKS_USER "arn:aws:eks:$AWS_REGION:$AWS_ACCOUNT_ID:cluster/$EKS_STACK_CLUSTER"
    _update_bazelrc_env LOGOS_AWS_REGISTRY "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"

    bazel run @logos//dev/logos/service/console

    await_console_pod

    jq --arg aws_account_id "$ACCOUNT" --arg region "$AWS_REGION" '.credHelpers |= . + {"public.ecr.aws": "ecr-login", "\($aws_account_id).dkr.ecr.\($region).amazonaws.com": "ecr-login"}' ~/.docker/config.json > ~/.docker/config.json.tmp && mv ~/.docker/config.json.tmp ~/.docker/config.json

    rds_setup

    dev_env
    _update_bazelrc_env STORAGE_PG_BACKEND_HOST "$PG_AUTH_RESOLVED_HOST"

    $BAZEL run -- @pnpm//:pnpm --dir "$BUILD_WORKSPACE_DIRECTORY" install
}

dev() {
    case "${1:-"help"}" in
        client)
            shift 1
            dev_client "$@"
            ;;
        env)
            shift 1
            dev_env "$@"
            ;;
        server)
            shift 1
            dev_server "$@"
            ;;
        *)
            help
            ;;
    esac
}

console_pod() {
    kubectl get pods -l app=console -o jsonpath="{.items[0].metadata.name}"
}

console() {
  kubectl exec -it "$(console_pod)" -- sh
}

rds_tunnel() {
    rds_tunnel_up
    sleep infinity
}

rds_tunnel_up() {
  CONSOLE_POD_NAME="$(console_pod)"
  kubectl exec "$CONSOLE_POD_NAME" -- sh -c 'killall socat || true'

  _run_with_pid "$PG_TUNNEL_SOCAT_PID" kubectl exec "$CONSOLE_POD_NAME" -- socat TCP-LISTEN:$PG_TUNNEL_REMOTE_PORT,fork,reuseaddr TCP:$PG_AUTH_HOST:$PG_TUNNEL_REMOTE_PORT
  sleep 3

  _run_with_pid "$PG_TUNNEL_PORT_FORWARD_PID" kubectl port-forward "$CONSOLE_POD_NAME" "$PG_TUNNEL_LOCAL_PORT:$PG_TUNNEL_REMOTE_PORT"

  stderr "Checking for tunnel connectivity."

  PGPASSWORD="$(rds_clusteradmin_password)"
  export PGPASSWORD

  until _psql --user clusteradmin -c "select" logos > /dev/null; do
    stderr "Database tunnel not ready."
    sleep 1
  done

  unset PGPASSWORD
}

rds_tunnel_down() {
    CONSOLE_POD_NAME="$(console_pod)"

    _kill_with_pid "$PG_TUNNEL_PORT_FORWARD_PID" "Disconnecting kubectl port forward."
    _kill_with_pid "$PG_TUNNEL_SOCAT_PID" "Killing kubectl exec socat."

    stderr "Killing socat in console pod."
    kubectl exec "$CONSOLE_POD_NAME" -- killall socat
}

rds_setup() {
    rds_tunnel_up
    PGPASSWORD="$(rds_clusteradmin_password)" _psql -U clusteradmin template1 \
<<SQL
    do language plpgsql \$\$
        begin
            if not exists (
                select from pg_roles where rolname = '$PG_DB_MIGRATION_USER'
            ) then
                execute 'create role $PG_DB_MIGRATION_USER';
            end if;
        end;
    \$\$;

    alter database logos owner to $PG_DB_MIGRATION_USER;

    alter role $PG_DB_MIGRATION_USER login;
    grant rds_iam to $PG_DB_MIGRATION_USER;
    grant rds_superuser to $PG_DB_MIGRATION_USER;
SQL
}

rds_token() {
    USERNAME=$1; shift 1

    aws rds generate-db-auth-token \
              --hostname "$PG_AUTH_RESOLVED_HOST" \
              --port "$PG_AUTH_PORT" \
              --region "$AWS_REGION" \
              --username "$USERNAME"
}

rds() {
    case "${1:-"help"}" in
        setup)
            shift 1
            rds_setup "$@"
            ;;
        token)
            shift 1
            dev_env
            rds_token "$@"
            ;;
        tunnel)
            shift 1
            rds_tunnel "$@"
            ;;
        *)
            help
            ;;
    esac
}

rds_clusteradmin_password() {
  aws secretsmanager \
      get-secret-value \
      --secret-id "$(aws secretsmanager list-secrets \
                                        --query "SecretList[?starts_with(Name, \`logosrdsstackdbclusterSecre\`)].Name | [0]" \
                                        --output text)" \
      --query "SecretString" \
      --output text | jq -r ".password"
}

deploy() {
    $BAZEL run @logos//dev/logos/stack/aws/cdk -- deploy --all --require-approval never
    $BAZEL run @logos//dev/logos/service/console

    dev_env

    $BAZEL run @logos//dev/logos/ingress/nginx
}

_psql() {
    dev_env
    PGPASSWORD="${PGPASSWORD:-$(rds_token "$PG_DB_MIGRATION_USER")}" \
        psql --host "$PG_TUNNEL_HOST" \
             --port "$PG_TUNNEL_PORT" \
             -U "$PG_DB_MIGRATION_USER" \
             "${@:-"logos"}"
}

logos() {
    case "${1:-"help"}" in
        console)
            shift 1
            console "$@"
            ;;
        setup)
            shift 1
            dev_setup "$@"
            ;;
        dev)
            shift 1
            dev "$@"
            ;;
        deploy)
            shift 1
            deploy "$@"
            ;;
        rds)
            shift 1
            rds "$@"
            ;;
        psql)
            shift 1
            rds_tunnel_up
            _psql "$@"
            ;;
        *)
            help
            ;;
    esac
}

help() {
    stderr
    stderr "$SELF \e[4mcommand\e[0m ..."
    stderr
    exit 1
}

logos "$@"
cleanup
