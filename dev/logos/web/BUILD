load("@aspect_rules_js//npm:defs.bzl", "npm_package")
load("//bzl:ts.bzl", "ts_library")

filegroup(
    name = "src",
    srcs = [
        "components/router-path.ts",
        "module/app-module.ts",
        "module/endpoint.ts",
        "module/user.ts",
        "storage/client.ts",
        "storage/entity/create.ts",
        "storage/entity/edit.ts",
        "storage/entity/list.ts",
        "storage/event.ts",
        "storage/index.ts",
    ],
    visibility = ["//visibility:public"],
)

ts_library(
    name = "web",
    srcs = [":src"],
    visibility = ["//visibility:public"],
    deps = [
        "//:node_modules/@spectrum-web-components/action-button",
        "//:node_modules/@spectrum-web-components/button",
        "//:node_modules/@spectrum-web-components/card",
        "//:node_modules/@spectrum-web-components/icons-workflow",
        "//:node_modules/@spectrum-web-components/reactive-controllers",
        "//:node_modules/@spectrum-web-components/textfield",
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
