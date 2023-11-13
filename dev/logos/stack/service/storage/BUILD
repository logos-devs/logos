load("//:gitops.bzl", "REGISTRY")
load("//bzl:k8s.bzl", "kubectl")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_push")
load("@rules_pkg//pkg:pkg.bzl", "pkg_tar")

java_library(
    name = "storage_library",
    srcs = [
        "EntityStorage.java",
        "EntityStorageService.java",
        "TableStorage.java",
        "exceptions/EntityReadException.java",
        "exceptions/EntityWriteException.java",
    ],
    visibility = [
        "//app:__subpackages__",
        "//dev/logos/app:__subpackages__",
        "//dev/logos/stack/bind:__pkg__",
        "//dev/logos/stack/service/backend:__subpackages__",
        "//dev/logos/stack/service/storage:__subpackages__",
    ],
    deps = [
        "//dev/logos/stack/service/storage/pg",
        "@com_google_protobuf//:protobuf_java",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//stub",
        "@maven//:com_google_inject_guice",
        "@maven//:com_querydsl_querydsl_core",
        "@maven//:com_querydsl_querydsl_sql",
        "@maven//:org_jdbi_jdbi3_core",
        "@maven//:org_jdbi_jdbi3_postgres",
        "@maven//:org_postgresql_postgresql",
    ],
)

oci_image(
    name = "image",
    base = "@stolon_container//image",
)

oci_push(
    name = "image_push",
    image = ":image",
)

kubectl(
    name = "storage",
    images = {":image_push": "logos-ecr-storage:latest"},
    manifests = [
        "manifests/role.yaml",
        "manifests/role-binding.yaml",
        "manifests/stolon-keeper.yaml",
        "manifests/stolon-proxy.yaml",
        "manifests/stolon-proxy-service.yaml",
        "manifests/stolon-sentinel.yaml",
    ],
    #    objects = [
    #        "//dev/logos/stack/service/envoy",
    #        "//dev/logos/stack/ingress/nginx",
    #        "//dev/logos/stack/service/backend",
    #    ],
    visibility = ["//dev/logos/stack:__subpackages__"],
)
