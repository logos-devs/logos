load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")
load("@rules_proto_grpc//java:defs.bzl", "java_proto_library")
load("//dev/logos/service/storage/pg/exporter:schema_export.bzl", "schema_export", "storage_grpc_web_library", "storage_java_proto_library")

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
