load("@aspect_rules_js//js:defs.bzl", "js_library")

js_library(
    name = "web",
    srcs = glob(["**/*.ts"]) + [
        "//app/summer/storage:schema_export",
    ],
    visibility = ["//dev/logos/stack/service/client/web:__pkg__"],
    deps = [
        "//dev/logos/stack/service/storage/pg/meta:export",
    ],
)
