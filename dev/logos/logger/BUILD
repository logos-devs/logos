java_library(
    name = "logger",
    srcs = [
        "LoggerFactory.java",
        "LoggerFactoryImpl.java",
        "LoggerModule.java"
    ],
    plugins = ["//dev/logos/app/register:module"],
    visibility = ["//visibility:public"],
    deps = [
        "//dev/logos/app/register:module_library",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:com_google_inject_extensions_guice_assistedinject",
        "@maven_logos//:org_slf4j_slf4j_api",
        "@maven_logos//:org_slf4j_slf4j_simple",
    ],
)

java_test(
    name = "test",
    srcs = ["LoggerModuleTest.java"],
    test_class = "dev.logos.logger.LoggerModuleTest",
    deps = [
        ":logger",
        "@maven_logos//:junit_junit",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:org_slf4j_slf4j_api",
    ],
)
