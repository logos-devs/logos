def module(
        name,
        srcs,
        plugins = None,
        resources = None,
        visibility = None,
        deps = None):
    native.java_library(
        name = name,
        srcs = srcs,
        plugins = ["@logos//dev/logos/app/register:module"] + (plugins or []),
        resources = resources,
        visibility = visibility,
        deps = [
            "@logos//dev/logos/app",
            "@logos//dev/logos/app/register:module_library",
            "@logos//dev/logos/module",
            "@maven_logos//:com_google_inject_guice",
            "@maven_logos//:io_grpc_grpc_inprocess",
            "@maven_logos//:software_amazon_awscdk_aws_cdk_lib",
            "@maven_logos//:software_constructs_constructs",
        ] + (deps or []),
    )
