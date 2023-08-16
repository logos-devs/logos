load("@com_adobe_rules_gitops//gitops:defs.bzl", gitops_k8s_deploy = "k8s_deploy")
load("//:cfg/aws.bzl", "ACCOUNT", "REGION")

NAMESPACE = "default"
EKS_STACK = "logos-eks"
#USER = "dev"

# TODO open bug with rules_gitops folks about not being able to use makevars here
#REGISTRY = LOCAL_REGISTRY

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

# TODO I need to extract the USER and CLUSTER to use from the
# most recent infra cdk deploy.

def k8s_deploy(**kwargs):
    if "image_repository" not in kwargs and "images" in kwargs:
        fail("image_repository is a required argument for k8s_deploy if images are supplied")

    gitops_k8s_deploy(
        namespace = NAMESPACE,
        cluster = CLUSTER,
        image_registry = REGISTRY,
        user = USER,
        #gitops = False,  # TODO toggle based upon environment with select. this is only valid for local dev.
        **kwargs
    )
