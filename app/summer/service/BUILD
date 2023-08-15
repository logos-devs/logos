load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")

java_grpc_library(
    name = "feed_java_grpc",
    protos = ["//app/summer/proto:feed_proto"],
)

java_library(
    name = "feed_service",
    srcs = glob(["**/*.java"]),
    visibility = [
        "//app/summer/module:__subpackages__",
        "//dev/logos/stack:__subpackages__",
    ],
    deps = [
        ":feed_java_grpc",
        "//app/summer/proto:feed_java_library",
    ],
)
