java_library(
    name = "service",
    srcs = ["Service.java"],
    visibility = [
        "//app:__subpackages__",
        "//dev/logos/service/backend:__subpackages__",
        "//dev/logos/service/storage:__subpackages__",
    ],
    deps = [
        "//dev/logos/service/storage/validator",
        "//dev/logos/user",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//stub",
    ],
)
