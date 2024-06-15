load("//bzl:ts.bzl", "ts_library")

ts_library(
    name = "web",
    srcs = glob(["**/*.ts"]) + [
        "//app/digits/storage:schema_export",
    ],
    visibility = ["//dev/logos/service/client/web:__pkg__"],
    deps = [
        "//:node_modules/@material/web",
        "//:node_modules/google-libphonenumber",
        "//:node_modules/inversify",
        "//:node_modules/lit",
        "//:node_modules/lit-html",
        "//app/digits/proto:grpc_web",
        "//app/digits/storage:grpc_web",
    ],
)
