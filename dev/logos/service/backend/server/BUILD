java_library(
    name = "server",
    srcs = [
        "GuardServerInterceptor.java",
        "ServerExecutor.java",
        "ServerModule.java",
    ],
    plugins = [
        "//dev/logos/app/register:module",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//dev/logos/app",
        "//dev/logos/app/register:module_library",
        "//dev/logos/module",
        "//dev/logos/service",
        "//dev/logos/service/backend/server/worker",
        "//dev/logos/service/storage/module",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:io_grpc_grpc_api",
        "@maven_logos//:io_grpc_grpc_inprocess",
        "@maven_logos//:io_grpc_grpc_netty_shaded",
        "@maven_logos//:io_grpc_grpc_services",
    ],
)

java_test(
    name = "test",
    srcs = ["GuardServerInterceptorTest.java"],
    test_class = "dev.logos.service.backend.server.GuardServerInterceptorTest",
    deps = [
        ":server",
        "//dev/logos/service",
        "@maven_logos//:io_grpc_grpc_api",
        "@maven_logos//:org_mockito_mockito_core",
        "@maven_logos//:junit_junit",
    ],
)
