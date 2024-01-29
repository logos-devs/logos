load("@rules_apko//apko:defs.bzl", "apko_image")

apko_image(
    name = "base_cronjob_image",
    architecture = "x86_64",
    config = "base_cronjob.yaml",
    contents = "@base_cronjob_image//:contents",
    output = "oci",
    tag = "latest",
    tags = ["requires-network"],
    visibility = ["//app:__subpackages__"],
)

apko_image(
    name = "envoy_image",
    architecture = "x86_64",
    config = "envoy.yaml",
    contents = "@envoy_image//:contents",
    output = "oci",
    tag = "latest",
    tags = ["requires-network"],
    visibility = ["//dev/logos/service/envoy:__pkg__"],
)
