load("//bzl:k8s.bzl", "kubectl")
load("//bzl:push_image.bzl", "push_image")
load("@rules_oci//oci:defs.bzl", "oci_image")

oci_image(
    name = "image",
    base = "@console_container",
    entrypoint = [
        "tail",
        "-f",
        "/dev/null",
    ],
)

push_image(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = "logos-ecr-console",
)

kubectl(
    name = "console",
    image_pushes = [":image_push"],
    images = {":image.digest": "logos-ecr-console"},
    manifests = [
        "manifests/deployment.yaml",
        "manifests/persistentvolumeclaim.yaml",
    ],
)
