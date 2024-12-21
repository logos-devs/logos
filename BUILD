load("@npm_logos//:defs.bzl", "npm_link_all_packages")
load("@aspect_rules_js//npm:defs.bzl", "npm_link_package")
load("@aspect_bazel_lib//lib:copy_to_bin.bzl", "copy_to_bin")

npm_link_all_packages(name = "node_modules")

copy_to_bin(
    name = "tsconfig",
    srcs = ["tsconfig.json"],
    visibility = [
        "//visibility:public",
    ],
)

exports_files(["package.json"])

npm_link_package(
    name = "node_modules/@logos/web",
    src = "@logos//dev/logos/web:npm",
)
