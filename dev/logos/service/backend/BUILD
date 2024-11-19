load("@rules_oci//oci:defs.bzl", "oci_image")
load("@aspect_bazel_lib//lib:copy_file.bzl", "copy_file")
load("@aspect_bazel_lib//lib:tar.bzl", "tar")
load("//bzl:k8s.bzl", "kubectl")
load("//bzl:push_image.bzl", "push_image")

copy_file(
    name = "cert_bundle",
    src = "@aws_rds_cert_bundle//file",
    out = "global-bundle.pem",
)

tar(
    name = "image_jar",
    srcs = [
        ":cert_bundle",
        "//dev/logos/service/backend/server:server_deploy.jar",
    ],
    mtree = [
        "etc/ssl/certs/aws-rds-global-bundle.pem uid=0 gid=0 mode=0444 type=file content=$(location :cert_bundle)",
        "server_deploy.jar uid=0 gid=0 mode=0444 type=file content=$(location //dev/logos/service/backend/server:server_deploy.jar)",
    ],
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
