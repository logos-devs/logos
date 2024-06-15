load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")
load("@rules_proto_grpc//java:defs.bzl", "java_proto_library")
load("//bzl:grpc_web.bzl", "grpc_web_client")

proto_library(
    name = "annotation_proto",
    srcs = ["annotation.proto"],
    visibility = ["//app/review:__subpackages__"],
)

proto_library(
    name = "file_proto",
    srcs = ["file.proto"],
    visibility = ["//app/review:__subpackages__"],
)

grpc_web_client(
    name = "annotation_grpc_web",
    proto = ":annotation_proto",
    visibility = ["//app/review/web:__subpackages__"],
)

grpc_web_client(
    name = "file_grpc_web",
    proto = ":file_proto",
    visibility = ["//app/review/web:__subpackages__"],
)

java_grpc_library(
    name = "annotation_java_grpc",
    protos = [":annotation_proto"],
    visibility = ["//visibility:public"],
)

java_grpc_library(
    name = "file_java_grpc",
    protos = [":file_proto"],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "annotation_java_library",
    protos = [":annotation_proto"],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "file_java_library",
    protos = [":file_proto"],
    visibility = ["//visibility:public"],
)
