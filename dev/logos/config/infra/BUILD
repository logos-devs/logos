load(":infra_flag.bzl", "infra_provider", "provider_class_cmd")

infra_provider(
    name = "provider",
    build_setting_default = "aws",
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

genrule(
    name = "infrastructure_provider_src",
    outs = ["InfrastructureProvider.java"],
    cmd = select({
        ":aws": provider_class_cmd("aws"),
        "//conditions:default": provider_class_cmd("aws"),
    }),
    visibility = ["//visibility:private"],
)

java_library(
    name = "infra_config",
    srcs = [":infrastructure_provider_src"],
    visibility = ["//visibility:public"],
)
