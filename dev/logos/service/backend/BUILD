load("@rules_oci//oci:defs.bzl", "oci_image", "oci_push")
load("@rules_pkg//pkg:pkg.bzl", "pkg_tar")
load("@aspect_bazel_lib//lib:tar.bzl", "tar")
load("//bzl:gitops.bzl", "REGISTRY")
load("//bzl:k8s.bzl", "kubectl")

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

oci_push(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = REGISTRY + "/" + "logos-ecr-backend",
)

kubectl(
    name = "backend",
    image_pushes = [":image_push"],
    images = {":image.digest": "logos-ecr-backend"},
    manifests = [
        "manifests/deployment.yaml",
        "manifests/service.yaml",
    ],
    visibility = [
        "//dev/logos/service:__subpackages__",
        "//dev/logos/service/ingress/nginx:__pkg__",
    ],
    deps = [
        "//dev/logos/service/database:db_rw_service"
    ],
)
