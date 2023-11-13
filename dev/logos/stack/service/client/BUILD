load("//:gitops.bzl", "REGISTRY")
load("//bzl:k8s.bzl", "kubectl")
load("@rules_oci//oci:defs.bzl", "oci_image", "oci_push")
load("@rules_pkg//pkg:pkg.bzl", "pkg_tar")

#container_image(
#    name = "image",
#    base = "@nginx_container//image",
#    cmd = [
#        "/usr/sbin/nginx",
#        "-c",
#        "/nginx.conf",
#    ],
#    directory = "/",
#    entrypoint = ["/usr/bin/env"],
#    files = [
#        "nginx.conf",
#        "//dev/logos/stack/service/client/web",
#    ],
#)
#
#k8s_deploy(
#    name = "client",
#    image_repository = "logos-ecr-client",
#    images = {
#        "client-image:latest": "//dev/logos/stack/service/client:image",
#    },
#    manifests = [
#        "manifests/deployment.yaml",
#        "manifests/service.yaml",
#    ],
#    objects = [
#        "//dev/logos/stack/service/envoy",
#        "//dev/logos/stack/ingress/nginx",
#        "//dev/logos/stack/service/backend",
#    ],
#    visibility = ["//dev/logos/stack:__subpackages__"],
#)

pkg_tar(
    name = "image_files",
    srcs = [
        "nginx.conf",
        "//dev/logos/stack/service/client/web",
    ],
)

oci_image(
    name = "image",
    base = "@nginx_container",
    entrypoint = [
        "/usr/sbin/nginx",
        "-c",
        "/nginx.conf",
    ],
    tars = [":image_files"],
)

oci_push(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = REGISTRY + "/" + "logos-ecr-client",
)

kubectl(
    name = "client",
    images = {":image_push": "logos-ecr-client:latest"},
    manifests = [
        "manifests/deployment.yaml",
        "manifests/service.yaml",
    ],
    #    objects = [
    #        "//dev/logos/stack/service/envoy",
    #        "//dev/logos/stack/ingress/nginx",
    #        "//dev/logos/stack/service/backend",
    #    ],
    visibility = ["//dev/logos/stack:__subpackages__"],
)
