load("//dev/logos/service/storage/pg/exporter:schema_export.bzl", "schema_export", "storage_grpc_web_library", "storage_java_proto_library")

schema_export(
    name = "schema_export",
    migrations = ["//app/digits/storage/migrations"],
    tables = {
        "digits": ["phone_number"],
    },
    visibility = ["//app/digits:__subpackages__"],
)

storage_java_proto_library(
    name = "storage",
    srcs = [":schema_export"],
    visibility = ["//app/digits:__subpackages__"],
)

storage_grpc_web_library(
    name = "grpc_web",
    srcs = [":schema_export"],
    visibility = ["//app/digits:__subpackages__"],
)
