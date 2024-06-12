load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc//java:defs.bzl", "java_proto_library")
load("//bzl:grpc_web.bzl", "grpc_web_client")

proto_library(
    name = "proto",
    srcs = [
        "auth.proto",
        "cognito.proto",
    ],
    visibility = [
        "//app/auth:__subpackages__",
        "//app/auth/web/client:__pkg__",
    ],
)

java_proto_library(
    name = "java",
    protos = [":proto"],
    visibility = [
        "//app/auth:__subpackages__",
    ],
)

grpc_web_client(
    name = "web",
    proto = ":proto",
    visibility = ["//app:__subpackages__"],
    #deps = ["//app/summer/storage:schema_export"],
)
