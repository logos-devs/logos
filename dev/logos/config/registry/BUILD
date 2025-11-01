load(":flags.bzl", "registry_prefix", "load_strategy")

registry_prefix(
    name = "prefix",
    build_setting_default = "",
    visibility = ["//visibility:public"],
)

load_strategy(
    name = "load_strategy",
    build_setting_default = "push",
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
