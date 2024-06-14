load("//bzl:ts.bzl", "ts_library")

ts_library(
    name = "web",
    srcs = ["review-editor.ts"],
    visibility = ["//dev/logos/service/client/web:__pkg__"],
    deps = [
        "//:node_modules/inversify",
        "//:node_modules/lit",
        "//app/review/proto:annotation_grpc_web",
        "//app/review/proto:file_grpc_web",
        "//app/review/storage:grpc_web",
    ],
)
