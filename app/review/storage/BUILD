load("//dev/logos/service/storage/pg/exporter:schema_export.bzl", "schema_export", "storage_grpc_web_library", "storage_java_proto_library")

schema_export(
    name = "schema_export",
    migrations = ["//app/review/storage/migrations"],
    tables = {
        "review": ["project"],
    },
)

storage_java_proto_library(
    name = "storage_library",
    srcs = [":schema_export"],
    visibility = ["//app/review:__subpackages__"],
)

storage_grpc_web_library(
    name = "grpc_web",
    srcs = [":schema_export"],
    visibility = ["//app/review:__subpackages__"],
)

java_library(
    name = "file_storage",
    srcs = [
        "FileStorage.java",
    ],
    visibility = ["//app/review:__subpackages__"],
    deps = [
        "//app/review/grammar:aipname_parser_java",
        "//app/review/grammar:java9_parser_java",
        "//app/review/proto:file_java_grpc",
        "//app/review/proto:file_java_library",
        "//app/review/storage:storage_library",
        "//dev/logos/service/storage:storage_library",
        "@maven//:org_antlr_antlr4_runtime",
    ],
)
