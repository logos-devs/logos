load("@npm_logos//:defs.bzl", "npm_link_all_packages")
load("@aspect_bazel_lib//lib:copy_to_bin.bzl", "copy_to_bin")
load("@aspect_rules_js//npm:defs.bzl", "npm_package")
load("@rules_apko//apko:defs.bzl", "apko_bazelrc")

npm_link_all_packages(name = "node_modules")

copy_to_bin(
    name = "tsconfig",
    srcs = ["tsconfig.json"],
    visibility = [
        "//visibility:public",
    ],
)

exports_files(["package.json"])

apko_bazelrc()

npm_package(
    name = "npm",
    srcs = [
        "//:package.json",
        "//dev/logos/web",
    ],
    package = "@logos/web",
    visibility = ["//visibility:public"],
)
