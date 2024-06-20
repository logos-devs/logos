load("//bzl:ts.bzl", "ts_library")
load("@aspect_rules_js//js:defs.bzl", "js_binary", "js_run_binary", "js_test")

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
    args = ["$(locations //third_party:busybox)"],
    data = [
        ":author_library",
        "//third_party:busybox",
    ],
    entry_point = "author.js",
)

js_run_binary(
    name = "author",
    outs = ["editor.txt"],
    tool = ":author_binary",
)
