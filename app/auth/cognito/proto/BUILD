load("@logos//bzl:proto.bzl", "java_grpc_library", "js_grpc_client")

proto_library(
    name = "proto",
    srcs = [
        "cognito.proto",
    ],
    visibility = [
        "//app:__subpackages__",
    ],
)

java_proto_library(
    name = "java_proto",
    visibility = [
        "//app/auth/cognito:__subpackages__",
    ],
    deps = [":proto"],
)

java_grpc_library(
    name = "java_grpc",
    srcs = [":proto"],
    visibility = [
        "//app/auth/cognito:__subpackages__",
    ],
    deps = [":java_proto"],
)

js_grpc_client(
    name = "js_grpc",
    proto = ":proto",
    visibility = ["//app/auth/cognito:__subpackages__"],
)