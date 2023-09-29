load("//dev/logos/stack/service/storage/pg/meta:schema_export.bzl", "schema_export", "storage_java_proto_library")

schema_export(
    name = "schema_export",
    tables = {
        "summer": [
            "entry",
            "source_rss",
        ],
    },
    visibility = ["//app/summer:__subpackages__"],
)

storage_java_proto_library(
    name = "storage_proto",
    srcs = [":schema_export"],
    visibility = ["//app/summer:__subpackages__"],
)
