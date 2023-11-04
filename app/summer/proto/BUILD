load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")

proto_library(
    name = "feed_proto",
    srcs = ["feed.proto"],
    visibility = [
        "//app/summer:__subpackages__",
    ],
    deps = ["//app/summer/storage:schema_export"],
)

java_grpc_library(
    name = "proto",
    protos = [":feed_proto"],
    visibility = [
        "//app/summer:__subpackages__",
    ],
)
