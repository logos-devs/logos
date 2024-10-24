def module(
        name,
        srcs,
        main_class,
        plugins = None,
        resources = None,
        visibility = None,
        deps = None):
    native.java_binary(
        name = name,
        srcs = srcs,
        main_class = main_class,
        plugins = ["@logos//dev/logos/app/register:module"] + (plugins or []),
        resources = resources,
        visibility = visibility,
        deps = [
            "@io_grpc_grpc_java//inprocess",
            "@logos//dev/logos/app",
            "@logos//dev/logos/app/register:module_library",
            "@logos//dev/logos/module",
            "@logos//dev/logos/service/backend/server",
            "@logos//dev/logos/service/storage/module",
            "@logos//dev/logos/stack/aws/module",
            "@maven_logos//:com_google_inject_guice",
            "@maven_logos//:software_amazon_awscdk_aws_cdk_lib",
            "@maven_logos//:software_constructs_constructs",
        ] + (deps or []),
    )
