load("//bzl:k8s.bzl", "kubectl")
load("//bzl:push_image.bzl", "push_image")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_push")

java_library(
    name = "storage_library",
    srcs = [
        "EntityStorage.java",
        "EntityStorageService.java",
        "TableStorage.java",
    ],
    visibility = [
        "//visibility:public",
    ],
    deps = [
        "//dev/logos/service",
        "//dev/logos/service/storage/exceptions",
        "//dev/logos/service/storage/pg",
        "//dev/logos/service/storage/validator",
        "//dev/logos/user",
        "@com_google_protobuf//:protobuf_java",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:io_grpc_grpc_api",
        "@maven_logos//:io_grpc_grpc_stub",
        "@maven_logos//:org_jdbi_jdbi3_core",
        "@maven_logos//:org_jdbi_jdbi3_postgres",
        "@maven_logos//:org_postgresql_postgresql",
    ],
)