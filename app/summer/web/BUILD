load("@aspect_rules_js//js:defs.bzl", "js_library")

js_library(
    name = "web",
    srcs = glob(["**/*.ts"]) + [
        "//app/summer/storage:grpc_web",
        "//app/summer/storage:schema_export",
        "//dev/logos/stack/aws:cognito_public_host_map",
    ],
    visibility = ["//dev/logos/service/client/web:__pkg__"],
    deps = [
        "//app/auth/proto:web",
        "//app/summer/proto:grpc_web",
        "//dev/logos/service/client/web/module",
        "//dev/logos/service/client/web/storage",
    ],
)
