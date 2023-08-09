load("@io_bazel_rules_docker//container:container.bzl", "container_image")
load("@io_bazel_rules_docker//contrib:test.bzl", "container_test")
load("//:gitops.bzl", "k8s_deploy")

container_image(
    name = "image",
    base = "@nginx_container//image",
    cmd = [
        "/usr/sbin/nginx",
        "-c",
        "/nginx.conf",
    ],
    directory = "/",
    entrypoint = ["/usr/bin/env"],
    files = [
        "nginx.conf",
        "//dev/logos/stack/service/client/web",
    ],
)

k8s_deploy(
    name = "client",
    image_repository = "logos-ecr-client",
    images = {
        "client-image:latest": "//dev/logos/stack/service/client:image",
    },
    manifests = [
        "manifests/deployment.yaml",
        "manifests/service.yaml",
    ],
    objects = [
        "//dev/logos/stack/service/envoy",
        "//dev/logos/stack/ingress/nginx",
        "//dev/logos/stack/service/backend",
    ],
    visibility = ["//dev/logos/stack:__subpackages__"],
)
