java_library(
    name = "module",
    srcs = [
        "K8sModule.java",
    ],
    plugins = ["//dev/logos/app/register:module"],
    visibility = ["//visibility:public"],
    deps = [
        "//dev/logos/app/register:module_library",
        "//dev/logos/module",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:org_cdk8s_cdk8s",
        "@maven_logos//:org_cdk8s_cdk8s_plus_30",
        "@maven_logos//:software_constructs_constructs",
    ],
)
