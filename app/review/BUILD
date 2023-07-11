load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")
load("@rules_proto_grpc//java:defs.bzl", "java_proto_library")

proto_library(
    name = "annotation_proto",
    srcs = ["proto/annotation.proto"],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "annotation_java_library",
    protos = [":annotation_proto"],
    visibility = ["//visibility:public"],
)

java_grpc_library(
    name = "annotation_java_grpc",
    protos = [":annotation_proto"],
    visibility = ["//visibility:public"],
)

proto_library(
    name = "file_proto",
    srcs = ["proto/file.proto"],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "file_java_library",
    protos = [":file_proto"],
    visibility = ["//visibility:public"],
)

java_grpc_library(
    name = "file_java_grpc",
    protos = [":file_proto"],
    visibility = ["//visibility:public"],
)

proto_library(
    name = "project_proto",
    srcs = ["proto/project.proto"],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "project_java_library",
    protos = [":project_proto"],
    visibility = ["//visibility:public"],
)

java_grpc_library(
    name = "project_java_grpc",
    protos = [":project_proto"],
    visibility = ["//visibility:public"],
)

java_library(
    name = "review_service",
    srcs = glob(["**/*.java"]),
    visibility = ["//dev/logos/stack:__subpackages__"],
    deps = [
        ":annotation_java_grpc",
        ":annotation_java_library",
        ":file_java_grpc",
        ":file_java_library",
        ":project_java_grpc",
        ":project_java_library",
        "//app/review/grammar:aipname_parser_java",
        "//app/review/grammar:java9_parser_java",
        "//dev/logos/stack/service/storage:storage_library",
        "//dev/logos/stack/service/storage/pg",
        "//dev/logos/stack/service/storage/pg/meta:schema_export_library",
        "@maven//:com_google_inject_guice",
        "@maven//:javax_inject_javax_inject",
        "@maven//:org_antlr_antlr4_runtime",
    ],
)
