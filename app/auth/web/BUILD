load("@aspect_rules_js//js:defs.bzl", "js_library")

js_library(
    name = "web",
    srcs = glob(["**/*.ts"]),
    visibility = ["//dev/logos/stack/service/client/web:__pkg__"],
    deps = ["//app/auth/web/client:auth_grpc_web_client"],
)
