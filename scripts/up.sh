#!/bin/bash -e

BAZEL="bazelisk"
ACCOUNT="$(aws sts get-caller-identity --query "Account" --output text)"
REGION="$(aws configure get region)"
STACK="logos-eks"

export AWS_DEFAULT_REGION="$REGION"

$BAZEL run //dev/logos/infra:cdk -- deploy --all

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

# k8s objects
#$bazel run //dev/logos/stack/service/client:client.apply
$BAZEL run //dev/logos/stack/service/debug:debug.apply