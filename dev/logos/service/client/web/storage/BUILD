load("@aspect_rules_js//js:defs.bzl", "js_library")

js_library(
    name = "storage",
    srcs = [
        "client.ts",
        "entity/create.ts",
        "entity/edit.ts",
        "entity/list.ts",
        "event.ts",
        "index.ts",
    ],
    visibility = ["//app:__subpackages__"],
    deps = ["//:node_modules"],
)
