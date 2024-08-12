load("//bzl:k8s.bzl", "kubectl")
load("//bzl:push_image.bzl", "push_image")
load("@rules_oci//oci:defs.bzl", "oci_image")
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
        # TODO leaving the above, but actually we don't need this at all since this was a hack to find a way to use cookies
        # TODO instead we're just going to put a traditional http server in to set cookies
        "/bin/bash",
        "-c",
        "apt-get update && apt-get install -y lua-cjson && envoy --config-path /config.yaml",
    ],
    tars = [":image_files"],
)

push_image(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = "logos-ecr-envoy",
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
