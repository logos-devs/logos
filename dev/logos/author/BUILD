load("//bzl:run_in_docker.bzl", "run_in_docker")
load("//bzl:ts.bzl", "ts_library")
load("@aspect_bazel_lib//lib:tar.bzl", "tar")
load("@aspect_rules_js//js:defs.bzl", "js_binary")

ts_library(
    name = "author_library",
    srcs = [
        "author.ts",
    ],
    deps = [
        "//:node_modules/inversify",
        "//:node_modules/node-gyp",
        "//:node_modules/node-pty",
        "//:node_modules/ollama",
        "//:node_modules/openai",
        "//:node_modules/winston",
    ],
)

js_binary(
    name = "author",
    data = [
        ":author_library",
    ],
    entry_point = "author.js",
)

run_in_docker(
    name = "shell",
    #    network = False,
    #docker run --rm -ti --hostname "logos-$(pwgen -s -A0 8 1)" author:latest bash
)
