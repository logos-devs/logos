java_library(
    name = "module",
    srcs = ["SummerModule.java"],
    visibility = [
        "//dev/logos/stack:__subpackages__",
    ],
    deps = [
        "//app/summer/service:entry_storage_service",
        "//dev/logos/app",
        "//dev/logos/app:app_module",
        "@io_grpc_grpc_java//api",
        "@maven//:com_google_inject_guice",
    ],
)
