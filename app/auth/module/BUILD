java_library(
    name = "module",
    srcs = ["AuthModule.java"],
    plugins = [
        "//dev/logos/app/register:module",
    ],
    resources = [
        "//dev/logos/stack/aws/cdk:cognito_hosts",
        "//dev/logos/stack/aws/cdk:cognito_server_config",
    ],
    visibility = ["//dev/logos/service:__subpackages__"],
    deps = [
        "//app/auth/interceptor/cognito",
        "//app/auth/module/data",
        "//app/auth/service",
        "//app/auth/user",
        "//dev/logos/app",
        "//dev/logos/app/register:module_library",
        "@io_grpc_grpc_java//api",
        "@maven_logos//:com_google_code_gson_gson",
        "@maven_logos//:com_google_guava_guava",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:software_amazon_awssdk_auth",
        "@maven_logos//:software_amazon_awssdk_secretsmanager",
        "@maven_logos//:software_amazon_awssdk_sts",
    ],
)
