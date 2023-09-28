java_library(
    name = "module",
    srcs = ["SummerModule.java"],
    visibility = [
        "//dev/logos/stack:__subpackages__",
    ],
    deps = [
        "//app/summer/service",
        "//app/summer/storage",
        "//app/summer/storage:storage_proto",
        "//dev/logos/app",
        "//dev/logos/app:app_module",
        "//dev/logos/stack/service/storage:storage_library",
        "@io_grpc_grpc_java//api",
        "@maven//:com_google_inject_guice",
    ],
)
