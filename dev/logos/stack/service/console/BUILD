load("@io_bazel_rules_docker//container:container.bzl", "container_image")
load("@io_bazel_rules_docker//docker/util:run.bzl", "container_run_and_commit")
load("//:gitops.bzl", "k8s_deploy")

container_image(
    name = "image",
    base = "@console_container//image",
    entrypoint = [
        "tail",
        "-f",
        "/dev/null",
    ],
)

k8s_deploy(
    name = "console",
    image_repository = "logos-ecr-console",
    images = {
        "console-image:latest": ":image",
    },
    manifests = ["manifests/deployment.yaml"],
)
