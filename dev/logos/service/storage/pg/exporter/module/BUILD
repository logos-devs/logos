java_library(
    name = "module",
    srcs = ["ExportModule.java"],
    visibility = ["//dev/logos/service/storage/pg:__subpackages__"],
    deps = [
        "//dev/logos/service/storage/pg/exporter/codegen/proto",
        "//dev/logos/service/storage/pg/exporter/codegen/service",
        "//dev/logos/service/storage/pg/exporter/codegen/type",
        "//dev/logos/service/storage/pg/exporter/mapper",
        "//dev/logos/service/storage/pg/exporter/module/annotation",
        "@com_google_protobuf//:protobuf_java",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:com_squareup_javapoet",
    ],
)
