java_library(
    name = "module",
    srcs = ["AuthModule.java"],
    visibility = ["//dev/logos/stack:__subpackages__"],
    deps = [
        "//app/auth/service:auth_service",
        "//app/auth/storage:auth_storage",
        "@io_grpc_grpc_java//api",
        "@maven//:com_google_inject_guice",
    ],
)
