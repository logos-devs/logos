load("//bzl:defs.bzl", "module")

module(
    name = "module",
    srcs = ["K8sModule.java"],
    visibility = ["//visibility:public"],
    deps = [
        "//app/auth/k8s/interceptor/serviceaccount",
        "//app/auth/k8s/machine",
        "//dev/logos/auth/machine/annotation",
        "@maven_logos//:io_grpc_grpc_api",
    ],
)
