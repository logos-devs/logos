load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")

java_library(
    name = "interceptor",
    srcs = ["AuthInterceptor.java"],
    visibility = [
        "//app/auth/module:__subpackages__",
        "//dev/logos/stack:__subpackages__",
    ],
    deps = [
        "@io_grpc_grpc_java//api",
    ],
)
