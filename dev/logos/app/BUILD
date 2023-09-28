java_library(
    name = "app",
    srcs = ["App.java"],
    visibility = ["//app:__subpackages__"],
    deps = [
        "@maven//:com_google_inject_guice",
    ],
)

java_library(
    name = "app_module",
    srcs = ["AppModule.java"],
    visibility = ["//app:__subpackages__"],
    deps = [
        "//dev/logos/stack/service/storage:storage_library",
        "@maven//:com_google_inject_guice",
        "@maven//:io_grpc_grpc_api",
    ],
)
