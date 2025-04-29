load("//bzl:defs.bzl", "module")

module(
    name = "module",
    srcs = ["DevModule.java"],
    visibility = ["//visibility:public"],
    deps = [
        "//app/auth/cognito/module/data",
        "//dev/logos/auth/machine/annotation",
        "//dev/logos/auth/user/annotation",
        "//dev/logos/stack/aws/module/annotation",
        "@maven_logos//:io_grpc_grpc_api",
        "@maven_logos//:software_amazon_awssdk_auth",
        "@maven_logos//:software_amazon_awssdk_cognitoidentityprovider",
        "@maven_logos//:software_amazon_awssdk_regions",
    ],
)
