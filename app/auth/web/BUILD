load("//bzl:ts.bzl", "ts_library")

ts_library(
    name = "web",
    srcs = [
        "interceptor/cognito-auth-interceptor.ts",
        "module/auth-module.ts",
        "state.ts",
    ],
    visibility = [
        "//dev/logos/service/client:__subpackages__",
    ],
    deps = [
        "//:node_modules/aws-jwt-verify",
        "//:node_modules/grpc-web",
        "//:node_modules/mobx",
        "//app/auth/proto:web",
        "//dev/logos/web",
    ],
)
