load("//bzl:ts.bzl", "ts_library")

ts_library(
    name = "module",
    srcs = [
        "app-module.ts",
        "endpoint.ts",
        "user.ts",
    ],
    visibility = [
        "//app:__subpackages__",
        "//dev/logos/service/client/web:__subpackages__",
    ],
    deps = [
        "//:node_modules/grpc-web",
        "//:node_modules/inversify",
        "//:node_modules/inversify-inject-decorators",
    ],
)
