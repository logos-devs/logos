load("@aspect_rules_js//js:defs.bzl", "js_library")

js_library(
    name = "module",
    srcs = ["app-module.ts"],
    visibility = [
        "//app:__subpackages__",
        "//dev/logos/service/client/web:__subpackages__",
    ],
    deps = [
        "//:node_modules/inversify",
    ],
)
