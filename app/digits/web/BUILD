load("@aspect_rules_js//js:defs.bzl", "js_library")

js_library(
    name = "web",
    srcs = glob(["**/*.ts"]) + [
        "//app/digits/storage:schema_export",
    ],
    visibility = ["//dev/logos/stack/service/client/web:__pkg__"],
    deps = [
        "//app/digits/web/client:voice_grpc_web_client",
        "//dev/logos/stack/service/storage/pg/meta:export",
    ],
)
