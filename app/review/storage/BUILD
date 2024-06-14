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
)

storage_grpc_web_library(
    name = "grpc_web",
    srcs = [":schema_export"],
    visibility = ["//app/review:__subpackages__"],
)
