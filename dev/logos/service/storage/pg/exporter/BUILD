java_library(
    name = "exporter",
    srcs = ["Exporter.java"],
    visibility = ["//visibility:public"],
    deps = [
        "//dev/logos/app/register:module_library",
        "//dev/logos/module",
        "//dev/logos/service/storage/module",
        "//dev/logos/service/storage/pg/exporter/codegen",
        "//dev/logos/service/storage/pg/exporter/codegen/proto",
        "//dev/logos/service/storage/pg/exporter/descriptor",
        "//dev/logos/service/storage/pg/exporter/module",
        "//dev/logos/service/storage/pg/exporter/module/annotation",
        "@maven_logos//:com_google_code_gson_gson",
        "@maven_logos//:com_google_guava_guava",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:com_squareup_javapoet",
    ],
)