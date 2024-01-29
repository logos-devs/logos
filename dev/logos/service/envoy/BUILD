load("//bzl:gitops.bzl", "REGISTRY")
load("//bzl:k8s.bzl", "kubectl")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_push")
load("@rules_pkg//pkg:pkg.bzl", "pkg_tar")

pkg_tar(
    name = "image_files",
    srcs = ["config.yaml"],
)

oci_image(
    name = "image",
    base = "@envoy_image_old",
    entrypoint = [
        # TODO hack! remove when we can build our own envoy in bazel
        "/bin/bash",
        "-c",
        "apt-get update && apt-get install -y lua-cjson && envoy --config-path /config.yaml",
    ],
    tars = [":image_files"],
)

oci_push(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = REGISTRY + "/" + "logos-ecr-envoy",
)

kubectl(
    name = "envoy",
    image_pushes = [":image_push"],
    images = {":image.digest": "logos-ecr-envoy"},
    manifests = [
        "manifests/deployment.yaml",
        "manifests/service.yaml",
    ],
    visibility = ["//dev/logos/ingress/nginx:__pkg__"],
    deps = ["//dev/logos/service/backend"],
)
