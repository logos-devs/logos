load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "pg",
    srcs = [
        "Column.java",
        "Converter.java",
        "Filter.java",
        "Identifier.java",
        "OrderBy.java",
        "QualifierFunction.java",
        "QualifierFunctionCall.java",
        "QualifierFunctionParameter.java",
        "Relation.java",
        "Result.java",
        "Schema.java",
        "Select.java",
        "SortOrder.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//dev/logos/auth/user",
        "//dev/logos/service",
        "//dev/logos/service/storage/exceptions",
        "//dev/logos/service/storage/validator",
        "@com_google_protobuf//:protobuf_java",
        "@maven_logos//:com_google_code_gson_gson",
        "@maven_logos//:com_google_guava_guava",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:io_grpc_grpc_api",
        "@maven_logos//:io_grpc_grpc_stub",
        "@maven_logos//:jakarta_inject_jakarta_inject_api",
        "@maven_logos//:org_jdbi_jdbi3_core",
        "@maven_logos//:org_jdbi_jdbi3_postgres",
    ],
)

java_test(
    name = "test",
    srcs = ["SelectTest.java"],
    test_class = "dev.logos.service.storage.pg.SelectTest",
    deps = [
        ":pg",
        "//dev/logos/service/storage/exceptions",
        "@maven_logos//:junit_junit",
        "@maven_logos//:org_jdbi_jdbi3_core",
        "@maven_logos//:org_mockito_mockito_core",
    ],
)
