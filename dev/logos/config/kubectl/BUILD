load(":flags.bzl", "kubectl_context")

kubectl_context(
    name = "context",
    build_setting_default = "",
    visibility = ["//visibility:public"],
)

config_setting(
    name = "aws",
    flag_values = {"//dev/logos/config/infra:provider": "aws"},
)

config_setting(
    name = "minikube",
    flag_values = {"//dev/logos/config/infra:provider": "minikube"},
)
