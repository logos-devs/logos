load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")
load("@rules_proto_grpc//js:defs.bzl", "js_grpc_web_compile")
load("//bzl:grpc_web.bzl", "grpc_web_client")

proto_library(
    name = "feed_proto",
    srcs = ["feed.proto"],
    visibility = [
        "//app/summer:__subpackages__",
    ],
    deps = [
        "//app/summer/storage:schema_export",
    ],
)

java_grpc_library(
    name = "proto",
    protos = [":feed_proto"],
    visibility = [
        "//app/summer:__subpackages__",
    ],
    deps = [
        "//app/summer/storage",
    ],
)

grpc_web_client(
    name = "grpc_web",
    proto = ":feed_proto",
    visibility = ["//app/summer/web:__pkg__"],
    deps = [
        "//app/summer/storage:schema_export",
    ],
)
