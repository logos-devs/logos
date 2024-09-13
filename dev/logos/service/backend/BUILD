load("@rules_oci//oci:defs.bzl", "oci_image")
load("@rules_pkg//pkg:pkg.bzl", "pkg_tar")
load("@aspect_bazel_lib//lib:tar.bzl", "tar")
load("//bzl:k8s.bzl", "kubectl")
load("//bzl:push_image.bzl", "push_image")

pkg_tar(
    name = "image_jar",
    srcs = ["//dev/logos/service/backend/server:server_deploy.jar"],
)

oci_image(
    name = "image",
    base = "@distroless_java_container",
    entrypoint = [
        "java",
        "-jar",
        "/server_deploy.jar",
    ],
    tars = [":image_jar"],
)

push_image(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = "logos-ecr-backend",
)

kubectl(
    name = "backend",
    image_pushes = [":image_push"],
    images = {":image.digest": "logos-ecr-backend"},
    manifests = [
        "manifests/deployment.yaml",
        "manifests/service.yaml",
    ],
    migrations = [
        "//dev/logos/service/storage/migrations",
    ],
    visibility = ["//dev/logos:__subpackages__"],
)
