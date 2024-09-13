load("//bzl:k8s.bzl", "kubectl")
load("//bzl:push_image.bzl", "push_image")
load("@rules_oci//oci:defs.bzl", "oci_image")
load("@rules_pkg//pkg:pkg.bzl", "pkg_tar")

pkg_tar(
    name = "image_files",
    srcs = ["config.yaml"],
)

oci_image(
    name = "image",
    base = "@envoy_image_old",
    entrypoint = [
        "/bin/bash",
        "-c",
        "envoy --config-path /config.yaml",
    ],
    tars = [":image_files"],
)

push_image(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = "logos-ecr-envoy",
)

kubectl(
    name = "envoy",
    image_pushes = [":image_push"],
    images = {":image.digest": "logos-ecr-envoy"},
    manifests = [
        "manifests/deployment.yaml",
        "manifests/service.yaml",
    ],
    visibility = ["//dev/logos:__subpackages__"],
)
