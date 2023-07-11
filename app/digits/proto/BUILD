load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc//java:defs.bzl", "java_proto_library")

proto_library(
    name = "voice_proto",
    srcs = ["voice.proto"],
    visibility = ["//app/digits:__subpackages__"],
)

java_proto_library(
    name = "voice_java_library",
    protos = ["//app/digits/proto:voice_proto"],
    visibility = [
        "//app/digits/module:__subpackages__",
        "//app/digits/service:__subpackages__",
        "//app/digits/storage:__subpackages__",
    ],
)
