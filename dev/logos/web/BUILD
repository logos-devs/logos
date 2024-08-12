load("//bzl:ts.bzl", "ts_library")

filegroup(
    name = "src",
    srcs = glob(["**/*.ts"]),
    visibility = ["//visibility:public"],
)

ts_library(
    name = "web",
    srcs = [":src"],
    visibility = ["//visibility:public"],
    deps = [
        "//:node_modules/grpc-web",
        "//:node_modules/lit",
        "//:node_modules/lit-html",
        "//:node_modules/inversify",
        "//:node_modules/inversify-inject-decorators",
        "//:node_modules/@material/web",
    ],
)
