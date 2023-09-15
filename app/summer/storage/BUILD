load("//dev/logos/stack/service/storage/pg/meta:schema_export.bzl", "schema_export", "storage_java_proto_library")

java_library(
    name = "storage",
    srcs = [
        "EntryStorage.java",
        "SourceRssStorage.java",
    ],
    visibility = ["//app/summer:__subpackages__"],
    deps = [
        ":storage_proto",
        "//dev/logos/stack/service/storage:storage_library",
        "//dev/logos/stack/service/storage/pg",
        "@com_google_protobuf//:protobuf_java",
        "@maven//:com_google_inject_guice",
        "@maven//:javax_inject_javax_inject",
    ],
)

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
