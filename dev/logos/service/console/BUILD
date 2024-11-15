load("//bzl:k8s.bzl", "kubectl")
load("//bzl:push_image.bzl", "push_image")
load("@aspect_bazel_lib//lib:tar.bzl", "tar")
load("@rules_oci//oci:defs.bzl", "oci_image")

tar(
    name = "rsyncd_conf",
    srcs = ["rsyncd.conf"],
    mtree = [
        "etc uid=0 gid=0 mode=0755 type=dir",
        "etc/rsyncd.conf uid=0 gid=0 mode=0600 time=0 type=file content={}/rsyncd.conf".format(package_name()),
    ],
)

oci_image(
    name = "image",
    base = "//vendor/apko:console_image",
    #    entrypoint = [
    #        "tail",
    #        "-f",
    #        "/dev/null",
    #    ],
    tars = [
        ":rsyncd_conf",
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
