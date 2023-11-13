load("@rules_python//python:defs.bzl", "py_binary")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_push")
load("@pip//:requirements.bzl", "requirement")
load("@rules_pkg//pkg:pkg.bzl", "pkg_tar")
load("//:gitops.bzl", "REGISTRY")
load("//bzl:k8s.bzl", "kubectl")

py_binary(
    name = "rss",
    srcs = ["rss.py"],
    deps = [
        requirement("boto3"),
        requirement("feedparser"),
        requirement("openai"),
        requirement("psycopg"),
        requirement("requests"),
        requirement("trafilatura"),
    ],
)

pkg_tar(
    name = "rss_tar",
    srcs = [":rss"],
)

oci_image(
    name = "image",
    base = "@alpine_image",
    entrypoint = [
        "python3",
        "rss.py",
    ],
    tars = [":rss_tar"],
)

oci_push(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = REGISTRY + "/" + "logos-ecr-feeds",
)

kubectl(
    name = "feeds",
    images = {":image_push": "logos-ecr-feeds:latest"},
    manifests = [
        "manifests/cronjob.yaml",
    ],
    visibility = ["//dev/logos/stack:__subpackages__"],
)
