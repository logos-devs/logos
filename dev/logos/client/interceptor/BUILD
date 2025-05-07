java_library(
    name = "interceptor",
    srcs = ["AuthTokenForwardingInterceptor.java"],
    visibility = ["//dev/logos/client/module:__subpackages__"],
    deps = [
        "//dev/logos/auth/credentials",
        "//dev/logos/auth/user",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:io_grpc_grpc_api",
    ],
)
