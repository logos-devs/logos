load("@aspect_rules_js//js:defs.bzl", "js_library")

js_library(
    name = "web",
    srcs = glob(["**/*.ts"]),
    visibility = ["//dev/logos/stack/service/client/web:__pkg__"],
    deps = [
        "//app/review/web/client:annotation_grpc_web_client",
        "//app/review/web/client:file_grpc_web_client",
        "//app/review/web/client:project_grpc_web_client",
    ],
)
