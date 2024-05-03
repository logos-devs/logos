load("@aspect_bazel_lib//lib:tar.bzl", "tar")
load("@pip_deps//:requirements.bzl", "requirement")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_push")
load("@rules_python//python:defs.bzl", "py_binary")
load("//bzl:gitops.bzl", "REGISTRY")
load("//bzl:k8s.bzl", "kubectl")

py_binary(
    name = "imap",
    srcs = ["imap.py"],
    main = "imap.py",
    deps = [
        requirement("chromadb"),
        requirement("ollama"),
        requirement("trafilatura")
    ],
)

tar(
    name = "image_tar",
    srcs = [":imap"],
)

oci_image(
    name = "image",
    base = "//vendor/apko:base_cronjob_image",
    entrypoint = ["/app/summer/jobs/email/imap"],
    tars = [
        ":image_tar",
    ],
)

oci_push(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = REGISTRY + "/" + "logos-ecr-email",
)

kubectl(
    name = "email",
    image_pushes = [":image_push"],
    images = {":image.digest": "logos-ecr-email"},
    manifests = [
        "manifests/cronjob.yaml",
    ],
    visibility = ["//dev/logos/service:__subpackages__"],
    deps = ["//dev/logos/service/database:db_rw_service"],
)
