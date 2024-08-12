java_library(
    name = "interceptor",
    srcs = ["GuardServerInterceptor.java"],
    visibility = [
        "//dev/logos/service/backend:__subpackages__",
    ],
    deps = [
        "//dev/logos/service",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//stub",
        "@maven_logos//:com_google_inject_guice",
    ],
)
