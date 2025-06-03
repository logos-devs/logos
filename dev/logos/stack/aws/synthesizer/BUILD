exports_files(["Synthesizer.java"])

java_binary(
    name = "synthesizer",
    srcs = [
        "Synthesizer.java",
    ],
    args = ["synth"],
    main_class = "dev.logos.stack.aws.synthesizer.Synthesizer",
    plugins = ["@logos//dev/logos/app/register:module"],
    visibility = ["//visibility:public"],
    tags = ["requires-network"],
    deps = [
        "//dev/logos/module",
        "//dev/logos/stack/aws/module",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:org_slf4j_slf4j_api",
        "@maven_logos//:org_slf4j_slf4j_simple",
        "@maven_logos//:software_amazon_awscdk_aws_cdk_lib",
    ],
)
