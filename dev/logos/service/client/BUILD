load("//bzl:k8s.bzl", "kubectl")
load("//bzl:push_image.bzl", "push_image")
load("@rules_oci//oci:defs.bzl", "oci_image")
load("@rules_pkg//pkg:pkg.bzl", "pkg_tar")
load("@rules_pkg//pkg:mappings.bzl", "pkg_files")

pkg_files(
    name = "image_files",
    srcs = [
        "web/index.html",
    ],
    prefix = "web",
)

pkg_tar(
    name = "image_tar",
    srcs = [
        "nginx.conf",
        ":image_files",
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
    tars = [":image_tar"],
)

push_image(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = "logos-ecr-client",
)

kubectl(
    name = "client",
    image_pushes = [":image_push"],
    images = select({
        "//dev/logos/config/infra:minikube": {},
        "//conditions:default": {":image.digest": "logos-ecr-client"},
    }),
    manifests = [
        "manifests/deployment.yaml",
        "manifests/service.yaml",
    ],
    visibility = ["//dev/logos:__subpackages__"],
)
