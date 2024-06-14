load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc//java:defs.bzl", "java_proto_library")
load("//bzl:grpc_web.bzl", "grpc_web_client")

proto_library(
    name = "proto",
    srcs = ["voice.proto"],
    visibility = ["//app/digits:__subpackages__"],
)

java_proto_library(
    name = "java_library",
    protos = [":proto"],
    visibility = [
        "//app/digits/module:__subpackages__",
        "//app/digits/service:__subpackages__",
        "//app/digits/storage:__subpackages__",
    ],
)

grpc_web_client(
    name = "grpc_web",
    proto = ":proto",
    visibility = ["//app/digits/web:__pkg__"],
)
