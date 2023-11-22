load("//bzl:gitops.bzl", "REGISTRY")
load("//bzl:k8s.bzl", "kubectl")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_push")
load("@rules_pkg//pkg:pkg.bzl", "pkg_tar")

pkg_tar(
    name = "image_files",
    srcs = [
        "nginx.conf",
        "//dev/logos/stack/service/client/web",
    ],
)

oci_image(
    name = "image",
    base = "@nginx_container",
    entrypoint = [
        "/usr/sbin/nginx",
        "-c",
        "/nginx.conf",
    ],
    tars = [":image_files"],
)

oci_push(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = REGISTRY + "/" + "logos-ecr-client",
)

kubectl(
    name = "client",
    image_pushes = [":image_push"],
    images = {":image.digest": "logos-ecr-client"},
    manifests = [
        "manifests/deployment.yaml",
        "manifests/service.yaml",
    ],
    visibility = ["//dev/logos/stack:__subpackages__"],
)
