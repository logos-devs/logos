load("@aspect_bazel_lib//lib:copy_file.bzl", "copy_file")
load("@aspect_bazel_lib//lib:tar.bzl", "tar")

copy_file(
    name = "cert_bundle",
    src = "@aws_rds_cert_bundle//file",
    out = "global-bundle.pem",
)

tar(
    name = "cert_bundle_layer",
    srcs = [
        ":cert_bundle",
    ],
    mtree = [
        "etc/ssl/certs/aws-rds-global-bundle.pem uid=0 gid=0 mode=0444 type=file content=$(location :cert_bundle)",
    ],
    visibility = ["//visibility:public"],
)
