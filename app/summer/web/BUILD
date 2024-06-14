load("//bzl:ts.bzl", "ts_library")

ts_library(
    name = "web",
    srcs = glob(["**/*.ts"]) + [
        "//dev/logos/stack/aws:cognito_public_host_map",
    ],
    visibility = ["//dev/logos/service/client/web:__pkg__"],
    deps = [
        "//:node_modules/inversify",
        "//:node_modules/@adobe/lit-mobx",
        "//app/auth/proto:web",
        # TODO : remove this and move the login-complete.ts to the auth app
        "//app/auth/web",
        "//app/summer/proto:grpc_web",
        "//app/summer/storage:grpc_web",
        "//dev/logos/service/client/web/module",
        "//dev/logos/service/client/web/storage",
    ],
)
