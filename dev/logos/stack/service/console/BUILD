load("//:gitops.bzl", "REGISTRY")
load("//bzl:k8s.bzl", "kubectl")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_push")

oci_image(
    name = "image",
    base = "@console_container",
    entrypoint = [
        "tail",
        "-f",
        "/dev/null",
    ],
)

oci_push(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = REGISTRY + "/" + "logos-ecr-console",
)

kubectl(
    name = "console",
    images = {":image_push": "logos-ecr-console:latest"},
    manifests = ["manifests/deployment.yaml"],
)
