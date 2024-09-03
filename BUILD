load("@npm_logos//:defs.bzl", "npm_link_all_packages")
load("@aspect_bazel_lib//lib:copy_to_bin.bzl", "copy_to_bin")
load("@rules_apko//apko:defs.bzl", "apko_bazelrc")

npm_link_all_packages(name = "node_modules")

copy_to_bin(
    name = "tsconfig",
    srcs = ["tsconfig.json"],
    visibility = [
        "//app:__subpackages__",
        "//dev/logos:__subpackages__",
    ],
)

apko_bazelrc()
