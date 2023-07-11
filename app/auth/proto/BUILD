load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc//java:defs.bzl", "java_proto_library")

proto_library(
    name = "auth_proto",
    srcs = ["auth.proto"],
    visibility = [
        "//app/auth:__subpackages__",
        "//app/auth/web/client:__pkg__",
    ],
)

java_proto_library(
    name = "auth_java_library",
    protos = [":auth_proto"],
    visibility = [
        "//app/auth:__subpackages__",
        # TODO make a way for app storage classes to register their enumerated types
        "//dev/logos/stack/storage:__subpackages__",
    ],
)
