load("//bzl:gitops.bzl", "REGISTRY")
load("//bzl:k8s.bzl", "kubectl")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_push")

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
        "//dev/logos/service:__subpackages__",
    ],
    deps = [
        "//dev/logos/service",
        "//dev/logos/service/storage/pg",
        "//dev/logos/service/storage/validator",
        "//dev/logos/user",
        "@com_google_protobuf//:protobuf_java",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//stub",
        "@maven//:com_google_inject_guice",
        "@maven//:org_jdbi_jdbi3_core",
        "@maven//:org_jdbi_jdbi3_postgres",
        "@maven//:org_postgresql_postgresql",
    ],
)

oci_image(
    name = "image",
    base = "@stolon_container",
)

oci_push(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = REGISTRY + "/" + "logos-ecr-storage",
)

kubectl(
    name = "storage",
    images = {":image.digest": "logos-ecr-storage"},
    manifests = [
        "manifests/role.yaml",
        "manifests/role-binding.yaml",
        "manifests/stolon-keeper.yaml",
        "manifests/stolon-proxy.yaml",
        "manifests/stolon-proxy-service.yaml",
        "manifests/stolon-sentinel.yaml",
    ],
    visibility = ["//dev/logos/service:__subpackages__"],
)
