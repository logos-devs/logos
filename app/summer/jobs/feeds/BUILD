load("@aspect_bazel_lib//lib:tar.bzl", "tar")
load("@pip_deps//:requirements.bzl", "requirement")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_push")
load("@rules_python//python:defs.bzl", "py_binary")
load("//bzl:gitops.bzl", "REGISTRY")
load("//bzl:k8s.bzl", "kubectl")

py_binary(
    name = "rss",
    srcs = ["rss.py"],
    main = "rss.py",
    deps = [
        requirement("boto3"),
        requirement("favicon"),
        requirement("feedparser"),
        requirement("lxml"),
        requirement("openai"),
        requirement("requests"),
        requirement("psycopg"),
        requirement("psycopg-binary"),
        requirement("pyinstaller"),
        requirement("trafilatura"),
        requirement("typing_extensions"),
    ],
)

tar(
    name = "image_tar",
    srcs = [":rss"],
)

oci_image(
    name = "image",
    base = "//vendor/apko:base_cronjob_image",
    entrypoint = ["/app/summer/jobs/feeds/rss"],
    tars = [
        ":image_tar",
    ],
)

oci_push(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = REGISTRY + "/" + "logos-ecr-feeds",
)

kubectl(
    name = "feeds",
    image_pushes = [":image_push"],
    images = {":image.digest": "logos-ecr-feeds"},
    manifests = [
        "manifests/cronjob.yaml",
    ],
    visibility = ["//dev/logos/stack:__subpackages__"],
    deps = ["//dev/logos/stack/service/database:db_rw_service"],
)
