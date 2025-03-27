java_library(
    name = "codegen",
    srcs = glob(["**/*.java"]),
    visibility = ["//dev/logos/service/storage/pg/exporter:__subpackages__"],
    deps = [
        "//dev/logos/app/register:module_library",
        "//dev/logos/service/storage:storage_library",
        "//dev/logos/service/storage/exceptions",
        "//dev/logos/service/storage/pg",
        "//dev/logos/service/storage/pg/exporter/codegen/type",
        "//dev/logos/service/storage/pg/exporter/descriptor",
        "//dev/logos/service/storage/pg/exporter/mapper",
        "//dev/logos/service/storage/validator",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:com_squareup_javapoet",
        "@maven_logos//:io_grpc_grpc_stub",
        "@maven_logos//:org_jdbi_jdbi3_core",
    ],
)
