load("//bzl:gitops.bzl", "REGISTRY")
load("//bzl:k8s.bzl", "kubectl")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_push")
load("@rules_pkg//pkg:pkg.bzl", "pkg_tar")

pkg_tar(
    name = "image_files",
    srcs = ["config.yaml"],
)

oci_image(
    name = "image",
    base = "@envoy_distroless_container",
    entrypoint = [
        "envoy",
        "--config-path",
        "/config.yaml",
    ],
    tars = [":image_files"],
)

oci_push(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = REGISTRY + "/" + "logos-ecr-envoy",
)

kubectl(
    name = "envoy",
    images = {":image.digest": "logos-ecr-envoy"},
    manifests = [
        "manifests/deployment.yaml",
        "manifests/service.yaml",
    ],
)
