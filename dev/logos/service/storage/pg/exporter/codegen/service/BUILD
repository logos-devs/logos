java_library(
    name = "service",
    srcs = ["StorageServiceBaseGenerator.java"],
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

java_test(
    name = "test",
    srcs = ["StorageServiceBaseGeneratorTest.java"],
    test_class = "dev.logos.service.storage.pg.exporter.codegen.service.StorageServiceBaseGeneratorTest",
    deps = [
        ":service",
        "//dev/logos/service/storage/pg/exporter/codegen/type",
        "//dev/logos/service/storage/pg/exporter/descriptor",
        "@maven_logos//:com_squareup_javapoet",
        "@maven_logos//:junit_junit",
        "@maven_logos//:org_mockito_mockito_core",
    ],
)
