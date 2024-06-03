load("//dev/logos/service/storage/pg/exporter:schema_export.bzl", "schema_export", "storage_grpc_web_library", "storage_java_proto_library")

schema_export(
    name = "export",
    tables = {
        "auth": ["credential"],
    },
)

storage_java_proto_library(
    name = "storage_library",
    srcs = [":export"],
)

java_library(
    name = "auth_storage",
    srcs = glob(["**/*.java"]),
    visibility = [
        "//app/auth/module:__subpackages__",
        "//app/auth/service:__subpackages__",
        "//dev/logos/service/backend:__subpackages__",
    ],
    deps = [
        ":storage_library",
        "//app/auth/proto:java",
        "//dev/logos/service/storage:storage_library",
        "//dev/logos/service/storage/pg",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_google_guava_guava",
        "@maven//:com_querydsl_querydsl_core",
        "@maven//:com_querydsl_querydsl_sql",
        "@maven//:com_yubico_webauthn_server_core",
        "@maven//:com_yubico_yubico_util",
        "@maven//:javax_json_javax_json_api",
        "@maven//:org_jdbi_jdbi3_core",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_slf4j_slf4j_simple",
    ],
)
