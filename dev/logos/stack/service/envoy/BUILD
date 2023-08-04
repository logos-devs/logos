load("@io_bazel_rules_docker//container:container.bzl", "container_image")
load("@io_bazel_rules_docker//contrib:test.bzl", "container_test")
load("@com_adobe_rules_gitops//gitops:defs.bzl", "k8s_deploy")
load("//:gitops.bzl", "k8s_deploy")

container_image(
    name = "image",
    base = "@envoy_distroless_container//image",
    cmd = [
        "--config-path",
        "/config.yaml",
    ],
    files = [":config.yaml"],
)

k8s_deploy(
    name = "envoy",
    image_repository = "logos-ecr-envoy",
    images = {
        "envoy-image:latest": ":image",
    },
    manifests = [
        "manifests/deployment.yaml",
        "manifests/service.yaml",
    ],
    visibility = ["//dev/logos/stack:__subpackages__"],
)
