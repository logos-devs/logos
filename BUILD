load("@com_github_bazelbuild_buildtools//buildifier:def.bzl", "buildifier")
load("@npm//:defs.bzl", "npm_link_all_packages")
load("@aspect_bazel_lib//lib:copy_to_bin.bzl", "copy_to_bin")

buildifier(name = "buildifier")

npm_link_all_packages(name = "node_modules")

copy_to_bin(
    name = "tsconfig",
    srcs = ["tsconfig.json"],
    visibility = ["//dev/logos:__subpackages__"],
)

exports_files(["requirements.txt"])

sh_binary(
    name = "minikube",
    srcs = select({
        "@platforms//os:osx": [
            "@minikube_osx//file",
        ],
        "@platforms//os:linux": [
            "@minikube_linux//file",
        ],
        "//conditions:default": ["@platforms//:incompatible"],
    }),
    tags = ["no-sandbox"],
)

sh_binary(
    name = "kubectl",
    srcs = select({
        "@platforms//os:osx": [
            "@kubectl_osx//file",
        ],
        "@platforms//os:linux": [
            "@kubectl_linux//file",
        ],
        "//conditions:default": ["@platforms//:incompatible"],
    }),
    tags = ["no-sandbox"],
)
