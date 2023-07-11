load("@com_adobe_rules_gitops//gitops:defs.bzl", "k8s_deploy")
load("@io_bazel_rules_docker//container:container.bzl", "container_image")
load("@io_bazel_rules_docker//contrib:test.bzl", "container_test")
load("//:gitops.bzl", "NAMESPACE", "REGISTRY", "USER")

java_library(
    name = "storage_library",
    srcs = glob([
        "**/*.java",
    ]),
    visibility = [
        "//app:__subpackages__",
        "//dev/logos/stack/bind:__pkg__",
        "//dev/logos/stack/service/backend:__subpackages__",
        "//dev/logos/stack/service/storage:__subpackages__",
    ],
    deps = [
        "//dev/logos/stack/service/storage/pg",
        "@com_google_protobuf//:protobuf_java",
        "@maven//:com_google_inject_guice",
        "@maven//:com_querydsl_querydsl_core",
        "@maven//:com_querydsl_querydsl_sql",
        "@maven//:org_jdbi_jdbi3_core",
        "@maven//:org_jdbi_jdbi3_postgres",
        "@maven//:org_postgresql_postgresql",
    ],
)

container_image(
    name = "image",
    base = "@stolon_container//image",
)

k8s_deploy(
    name = "storage",
    image_registry = REGISTRY,
    images = {
        "storage-image:latest": ":image",
    },
    manifests = [
        "manifests/role.yaml",
        "manifests/role-binding.yaml",
        "manifests/stolon-keeper.yaml",
        "manifests/stolon-proxy.yaml",
        "manifests/stolon-proxy-service.yaml",
        "manifests/stolon-sentinel.yaml",
    ],
    namespace = NAMESPACE,
    user = USER,
    visibility = ["//dev/logos/stack:__subpackages__"],
)
