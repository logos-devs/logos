load("@rules_python//python:defs.bzl", "py_binary")
load("@pip//:requirements.bzl", "requirement")
load("//:gitops.bzl", "k8s_deploy")
load("@io_bazel_rules_docker//container:container.bzl", "container", "container_image")
load("@io_bazel_rules_docker//docker/util:run.bzl", "container_run_and_commit")

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

container_image(
    name = "base_image",
    base = "@alpine//image",
    files = [
        ":rss.py",
        "//:requirements.txt",
    ],
)

container_run_and_commit(
    name = "image_setup",
    commands = [
        "apk update",
        "apk add libpq-dev python3 py3-pip",
        "pip3 install -r requirements.txt",
        "pip install psycopg[binary]",
    ],
    image = ":base_image.tar",
)

container_image(
    name = "image",
    base = ":image_setup",
    cmd = "python3 rss.py",
)

k8s_deploy(
    name = "feeds",
    image_repository = "logos-ecr-feeds",
    images = {
        "feeds-image:latest": ":image",
    },
    manifests = [
        "manifests/cronjob.yaml",
    ],
)
