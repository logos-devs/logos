#!/bin/bash -e

WORKSPACE_DIR="$(dirname "$(realpath "$0")")/.."

die() {
    echo "$@" 2>&1
    exit 1
}

[ -f "$WORKSPACE_DIR"/gitops_ecr.bzl ] || (echo 'REPOSITORIES = {}' > "$WORKSPACE_DIR"/gitops_ecr.bzl)

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


CONSOLE_POD_NAME="$(kubectl get pods -l app=console -o jsonpath="{.items[0].metadata.name}")"

kubectl wait --for=condition=Ready "pod/$CONSOLE_POD_NAME"

kubectl port-forward "$CONSOLE_POD_NAME" 15432:5432 &
PORT_FORWARD_PID=$!
sleep 3

kubectl exec "$CONSOLE_POD_NAME" -- socat TCP-LISTEN:5432,fork,reuseaddr TCP:db-rw.logos.dev:5432 &
SOCAT_PID=$!
sleep 3

SECRET_NAME="$(aws secretsmanager list-secrets --query "SecretList[?starts_with(Name, \`logosrdsclusterSecret\`)].Name | [0]" --output text)"
SQUITCH_PASSWORD="$(aws secretsmanager get-secret-value --secret-id "$SECRET_NAME" --query "SecretString" --output text | jq -r ".password")"

export SQUITCH_PASSWORD
export PGPASSWORD="$SQUITCH_PASSWORD"

pushd dev/logos/stack/service/storage/migrations
sqitch deploy -t logos
popd

unset SQITCH_PASSWORD

kill "$PORT_FORWARD_PID"
kill "$SOCAT_PID"
kubectl exec "$CONSOLE_POD_NAME" -- pkill -SIGQUIT socat

#$bazel run //dev/logos/stack/service/client:client.apply