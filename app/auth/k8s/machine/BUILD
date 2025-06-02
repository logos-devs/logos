java_library(
    name = "machine",
    srcs = [
        "ServiceAccountMachine.java",
    ],
    visibility = [
        "//visibility:public",
    ],
    deps = [
        "//dev/logos/auth/machine",
        "@maven_logos//:io_grpc_grpc_api",
        "@maven_logos//:io_jsonwebtoken_jjwt_api",
    ],
)

java_test(
    name = "test",
    srcs = ["ServiceAccountMachineTest.java"],
    test_class = "app.auth.k8s.machine.ServiceAccountMachineTest",
    deps = [
        ":machine",
        "@maven_logos//:io_jsonwebtoken_jjwt_api",
        "@maven_logos//:io_jsonwebtoken_jjwt_impl",
        "@maven_logos//:io_jsonwebtoken_jjwt_jackson",
        "@maven_logos//:org_mockito_mockito_core",
        "@maven_logos//:junit_junit",
    ],
)
