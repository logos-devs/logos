load("//:cfg/aws.bzl", "ACCOUNT", "REGION")

NAMESPACE = "default"
EKS_STACK = "logos-eks"
#USER = "dev"

REGISTRY = "{account}.dkr.ecr.{region}.amazonaws.com".format(
    account = ACCOUNT,
    region = REGION,
)

USER = "arn:aws:eks:{region}:{account}:cluster/{stack}".format(
    account = ACCOUNT,
    region = REGION,
    stack = EKS_STACK,
)

CLUSTER = "arn:aws:eks:{region}:{account}:cluster/{stack}".format(
    account = ACCOUNT,
    region = REGION,
    stack = EKS_STACK,
)
