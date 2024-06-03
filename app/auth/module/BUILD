java_library(
    name = "module",
    srcs = ["AuthModule.java"],
    plugins = [
        "//dev/logos/app/register:module",
    ],
    resources = [
        "//dev/logos/infra:cognito_hosts",
        "//dev/logos/infra:cognito_server_config",
    ],
    visibility = ["//dev/logos/service:__subpackages__"],
    deps = [
        "//app/auth/interceptor/cognito",
        "//app/auth/module/data",
        "//app/auth/service",
        "//app/auth/storage:auth_storage",
        "//app/auth/user",
        "//dev/logos/app",
        "//dev/logos/app/register:module_library",
        "@io_grpc_grpc_java//api",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_inject_guice",
        "@maven//:software_amazon_awssdk_secretsmanager",
    ],
)
