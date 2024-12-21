load("@aspect_rules_js//npm:defs.bzl", "npm_package")
load("//bzl:ts.bzl", "ts_library")

filegroup(
    name = "src",
    srcs = [
        "components/router-path.ts",
        "module/app-module.ts",
        "module/endpoint.ts",
        "module/user.ts",
    ],
    visibility = ["//visibility:public"],
)

ts_library(
    name = "web",
    srcs = [":src"],
    visibility = ["//visibility:public"],
    deps = [
        "//:node_modules/grpc-web",
        "//:node_modules/inversify",
        "//:node_modules/inversify-inject-decorators",
        "//:node_modules/lit",
        "//:node_modules/path-to-regexp",
    ],
)

npm_package(
    name = "npm",
    srcs = [
        ":web",
        "//:package.json",
    ],
    package = "@logos/web",
    visibility = ["//visibility:public"],
)
