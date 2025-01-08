load("@aspect_rules_js//js:defs.bzl", "js_run_binary")
load("@npm_logos//:aws-cdk/package_json.bzl", cdk = "bin")

def aws_cdk_synthesizer(name, deps = None, visibility = None):
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
        deps = [
            "@logos//dev/logos/module",
            "@logos//dev/logos/stack/aws/module",
            "@maven_logos//:com_google_inject_guice",
            "@maven_logos//:org_slf4j_slf4j_api",
            "@maven_logos//:org_slf4j_slf4j_simple",
            "@maven_logos//:software_amazon_awscdk_aws_cdk_lib",
        ] + deps,
        visibility = visibility,
    )

def aws(name, deps = None, visibility = None):
    aws_cdk_synthesizer(name = name + "_synthesizer", deps = deps, visibility = visibility)

    cdk.cdk_binary(
        name = name,
        data = [
            ":" + name + "_synthesizer_deploy.jar",
        ],
        expand_args = True,
        chdir = native.package_name(),
        fixed_args = [
            "--app",
            "'java -jar $$(realpath " + name + "_synthesizer_deploy.jar)'",
        ],
        tags = ["no-sandbox"],
        visibility = visibility,
    )

    js_run_binary(
        name = name + "_outputs",
        outs = ["stack.json"],
        args = [
            "deploy",
            "-v",
            "--all",
            "--outputs-file",
            "stack.json",
            "--require-approval",
            "never",
        ],
        log_level = "debug",
        mnemonic = "CdkDeploy",
        progress_message = "Deploying to AWS",
        silent_on_success = False,
        tags = [
            "no-remote",
            "no-sandbox",
        ],
        tool = ":" + name,
        visibility = visibility,
    )
