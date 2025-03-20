java_binary(
    name = "synthesizer",
    srcs = [
        "Synthesizer.java",
    ],
    main_class = "dev.logos.stack.k8s.synthesizer.Synthesizer",
    plugins = ["@logos//dev/logos/app/register:module"],
    visibility = ["//visibility:public"],
    deps = [
        "//dev/logos/module",
        "//dev/logos/service",
        "//dev/logos/stack/k8s/module",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:org_cdk8s_cdk8s",
        "@maven_logos//:org_cdk8s_cdk8s_plus_30",
        "@maven_logos//:org_slf4j_slf4j_api",
        "@maven_logos//:org_slf4j_slf4j_simple",
        "@maven_logos//:software_constructs_constructs",
    ],
)

genrule(
    name = "synth",
    outs = ["stack-chart.k8s.yaml"],
    cmd = "$(location :synthesizer) && mv dist/* $(@D)/ && rm -rf dist",
    exec_properties = {"cdk_lock": "1"},
    tags = ["no-sandbox"],
    tools = [":synthesizer"],
    visibility = ["//visibility:public"],
)
