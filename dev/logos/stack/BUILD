load("@com_adobe_rules_gitops//gitops:defs.bzl", "k8s_deploy")
load("//:gitops.bzl", "k8s_deploy")

k8s_deploy(
    name = "hot",
    objects = [
        "//dev/logos/stack/service/backend:service_hot",
        "//dev/logos/stack/service/client:service_hot",
    ],
)

k8s_deploy(
    name = "stack",
    objects = [
        "//dev/logos/stack/service/backend:backend",
        "//dev/logos/stack/service/client:client",
        "//dev/logos/stack/service/envoy:envoy",
        "//dev/logos/stack/service/storage:storage",
        "//dev/logos/stack/ingress/nginx:nginx",
    ],
)
