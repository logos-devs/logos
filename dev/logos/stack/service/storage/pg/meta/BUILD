load(":schema_export.bzl", "schema_export")

java_binary(
    name = "exporter",
    srcs = [
        "Exporter.java",
    ],
    main_class = "dev.logos.stack.service.storage.exporter.Exporter",
    visibility = ["//app:__subpackages__"],
    deps = [
        "//dev/logos/stack/module:database_module",
        "//dev/logos/stack/service/storage:storage_library",
        "//dev/logos/stack/service/storage/pg",
        "@com_google_protobuf//java/core",
        "@maven//:aopalliance_aopalliance",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_failureaccess",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_inject_guice",
        "@maven//:com_querydsl_querydsl_codegen",
        "@maven//:com_querydsl_querydsl_sql",
        "@maven//:com_querydsl_querydsl_sql_codegen",
        "@maven//:com_squareup_javapoet",
        "@maven//:javax_inject_javax_inject",
        "@maven//:org_jdbi_jdbi3_core",
        "@maven//:org_jdbi_jdbi3_postgres",
        "@maven//:org_postgresql_postgresql",
    ],
)

schema_export(
    name = "export",
    tables = {
        "auth": ["credential"],
        "review": ["project"],
    },
    visibility = ["//app:__subpackages__"],
)

java_library(
    name = "schema_export_library",
    srcs = [
        ":export",
    ],
    visibility = ["//app:__subpackages__"],
    deps = [
        "//dev/logos/stack/service/storage/pg",
        "@com_google_protobuf//java/core",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//stub",
        "@maven//:com_google_guava_guava",
        "@maven//:com_querydsl_querydsl_core",
        "@maven//:com_querydsl_querydsl_sql",
        "@maven//:io_grpc_grpc_protobuf",
        "@maven//:javax_annotation_javax_annotation_api",
        "@maven//:javax_inject_javax_inject",
    ],
)
