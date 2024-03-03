java_library(
    name = "app",
    srcs = [
        "App.java",
        "AppModule.java",
    ],
    visibility = ["//app:__subpackages__"],
    deps = [
        "//dev/logos/service",
        "@io_grpc_grpc_java//api",
        "@maven//:com_google_inject_guice",
    ],
)
