load("//bzl:ts.bzl", "ts_library")
load("@aspect_rules_js//js:defs.bzl", "js_binary", "js_run_binary", "js_test")
load("@aspect_bazel_lib//lib:tar.bzl", "tar")
load("//bzl:run_in_ns.bzl", "run_in_ns")

tar(
    name = "root",
    srcs = [
        "@busybox//:busybox_static",
    ],
    out = "busybox.tar",
    mtree = [
        "usr type=dir uid=0 gid=0 mode=0755",
        "usr/bin type=dir uid=0 gid=0 mode=0755",
        "usr/bin/busybox type=file uid=0 gid=0 mode=0755 contents=$(location @busybox//:busybox_static)",
    ],
)

run_in_ns(
    name = "shell",
    cmd = "echo Hello, World!",
    root = ":root",
)

ts_library(
    name = "author_library",
    srcs = [
        "author.ts",
    ],
    deps = [
        "//:node_modules/ollama",
    ],
)

js_binary(
    name = "author_binary",
    args = ["$(locations @busybox//:busybox_static)"],
    data = [
        ":author_library",
        "@busybox//:busybox_static",
    ],
    entry_point = "author.js",
)

js_run_binary(
    name = "author",
    outs = ["editor.txt"],
    tool = ":author_binary",
)
