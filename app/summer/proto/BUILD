load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc//java:defs.bzl", "java_proto_library")

proto_library(
    name = "feed_proto",
    srcs = ["feed.proto"],
    visibility = [
        "//app/summer:__subpackages__",
    ],
)

java_proto_library(
    name = "feed_java_library",
    protos = [":feed_proto"],
    visibility = [
        "//app/summer:__subpackages__",
    ],
)
