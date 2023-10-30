java_library(
    name = "module",
    srcs = ["SummerModule.java"],
    visibility = [
        "//dev/logos/stack:__subpackages__",
    ],
    deps = [
        "//app/summer/service",
        "//app/summer/storage",
        "//dev/logos/app",
        "//dev/logos/stack/service/storage:storage_library",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//stub",
        "@maven//:com_google_inject_guice",
    ],
)
