load("@com_adobe_rules_gitops//gitops:defs.bzl", gitops_k8s_deploy = "k8s_deploy")
load("//:gitops_local.bzl", "ACCOUNT", "REGION", "STACK")

NAMESPACE = "default"
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
    stack = STACK,
)

CLUSTER = "arn:aws:eks:{region}:{account}:cluster/{stack}".format(
    account = ACCOUNT,
    region = REGION,
    stack = STACK,
)

# TODO I need to extract the USER and CLUSTER to use from the
# most recent infra cdk deploy.

def k8s_deploy(*args, **kwargs):
    gitops_k8s_deploy(
        namespace = NAMESPACE,
        image_registry = REGISTRY,
        cluster = CLUSTER,
        user = USER,
        #gitops = False,  # TODO toggle based upon environment with select. this is only valid for local dev.
        *args,
        **kwargs
    )
