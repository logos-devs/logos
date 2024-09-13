load("@npm_logos//:aws-cdk/package_json.bzl", cdk = "bin")

def aws_cdk_synthesizer(name, deps = None):
    if deps == None:
        deps = []

    native.java_binary(
        name = name,
        srcs = [
            "@logos//dev/logos/stack/aws/synthesizer:Synthesizer.java",
        ],
        args = ["synth"],
        main_class = "dev.logos.stack.aws.synthesizer.Synthesizer",
        plugins = ["@logos//dev/logos/app/register:module"],
        visibility = ["//visibility:public"],
        deps = [
            "@logos//dev/logos/module",
            "@maven_logos//:com_google_inject_guice",
            "@maven_logos//:org_slf4j_slf4j_api",
            "@maven_logos//:org_slf4j_slf4j_simple",
            "@maven_logos//:software_amazon_awscdk_aws_cdk_lib",
        ] + deps,
    )

def aws(name, deps = None):
    aws_cdk_synthesizer(name = name + "_synthesizer", deps = deps)

    cdk.cdk_binary(
        name = name,
        data = [
            name + "_synthesizer_deploy.jar",
        ],
        expand_args = True,
        fixed_args = [
            "--app",
            "'SYNTH_JAR=\"$(location :" + name + "_synthesizer_deploy.jar)\"; java -cp \"$${SYNTH_JAR#bazel-out/k8-fastbuild/bin/}\" dev.logos.stack.aws.synthesizer.Synthesizer'",
        ],
        tags = ["no-sandbox"],
        visibility = ["//visibility:public"],
    )
