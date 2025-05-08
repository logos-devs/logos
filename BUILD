load("@npm_logos//:defs.bzl", "npm_link_all_packages")
load("@aspect_rules_js//npm:defs.bzl", "npm_link_package")
load("@aspect_bazel_lib//lib:copy_to_bin.bzl", "copy_to_bin")
load("@rules_java//toolchains:default_java_toolchain.bzl", "DEFAULT_TOOLCHAIN_CONFIGURATION", "default_java_toolchain")

default_java_toolchain(
    name = "corretto23_toolchain",
    configuration = DEFAULT_TOOLCHAIN_CONFIGURATION,
    java_runtime = "@corretto23//:jdk",
    source_version = "23",
    target_version = "23",
)

toolchain(
    name = "java_toolchain",
    toolchain = ":corretto23_toolchain",
    toolchain_type = "@bazel_tools//tools/jdk:toolchain_type",
    visibility = ["//visibility:public"],
)

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

config_setting(
    name = "deployment_target_dev",
    values = {"define": "deployment_target=dev"},
)

config_setting(
    name = "deployment_target_test",
    values = {"define": "deployment_target=test"},
)

config_setting(
    name = "deployment_target_stage",
    values = {"define": "deployment_target=stage"},
)

config_setting(
    name = "deployment_target_prod",
    values = {"define": "deployment_target=prod"},
)
