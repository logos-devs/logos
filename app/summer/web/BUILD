load("@aspect_rules_js//js:defs.bzl", "js_library")

js_library(
    name = "web",
    srcs = glob(["**/*.ts"]) + [
        "//app/summer/storage:schema_export",
    ],
    visibility = ["//dev/logos/service/client/web:__pkg__"],
    deps = [
        "//app/auth/proto:web",
        "//app/summer/proto:grpc_web",
        "//dev/logos/service/storage/pg/exporter:export",
    ],
)
