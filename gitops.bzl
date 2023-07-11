load("@com_adobe_rules_gitops//gitops:defs.bzl", gitops_k8s_deploy = "k8s_deploy")
load("//:gitops_local.bzl", "LOCAL_REGISTRY")

NAMESPACE = "default"

# TODO open bug with rules_gitops folks about not being able to use makevars here
REGISTRY = LOCAL_REGISTRY
USER = "dev"

def k8s_deploy(*args, **kwargs):
    gitops_k8s_deploy(
        namespace = NAMESPACE,
        image_registry = REGISTRY,
        user = USER,
        gitops = False,  # TODO toggle based upon environment with select. this is only valid for local dev.
        *args,
        **kwargs
    )
