load("@io_bazel_rules_docker//container:container.bzl", "container_image")
load("@io_bazel_rules_docker//docker/util:run.bzl", "container_run_and_commit")
load("//:gitops.bzl", "k8s_deploy")

container_image(
    name = "image_base",
    base = "@console_container//image",
)

container_run_and_commit(
    name = "image_commands",
    commands = [
        "echo http://mirrors.gigenet.com/alpinelinux/v3.16/main > /etc/apk/repositories",
        "echo http://mirrors.gigenet.com/alpinelinux/v3.16/community >> /etc/apk/repositories",
        "apk add bind-tools ca-certificates curl postgresql-client socat",
    ],
    image = ":image_base.tar",
    tags = ["no-remote"],
)

container_image(
    name = "image",
    base = ":image_commands_commit.tar",
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
