load("@aspect_rules_js//js:defs.bzl", "js_run_binary")
load("@npm_logos//:aws-cdk/package_json.bzl", cdk = "bin")

def aws_cdk_synthesizer(name, deps = None, visibility = None):
    if deps == None:
        deps = []

    native.java_binary(
        name = name,
        srcs = ["@logos//dev/logos/stack/aws/synthesizer:Synthesizer.java"],
        main_class = "dev.logos.stack.aws.synthesizer.Synthesizer",
        plugins = ["@logos//dev/logos/app/register:module"],
        deps = [
            "@logos//dev/logos/module",
            "@logos//dev/logos/stack/aws/module",
            "@maven_logos//:com_google_inject_guice",
            "@maven_logos//:org_slf4j_slf4j_api",
            "@maven_logos//:org_slf4j_slf4j_simple",
            "@maven_logos//:software_amazon_awscdk_aws_cdk_lib",
            "@maven_logos//:software_amazon_awssdk_ec2",
            "@maven_logos//:software_amazon_awssdk_sso",
            "@maven_logos//:software_amazon_awssdk_ssooidc",
            "@maven_logos//:software_amazon_awssdk_sts",
        ] + deps,
        visibility = visibility,
    )

def _aws_stack_zip_impl(ctx):
    stack_zip = ctx.actions.declare_file("stack.zip")
    outputs = [stack_zip]

    tool_inputs, tool_input_manifests = ctx.resolve_tools(tools = [ctx.attr.synthesizer])
    all_inputs = depset([ctx.executable.synthesizer], transitive = [tool_inputs])

    ctx.actions.run_shell(
        use_default_shell_env = True,
        command = """{synthesizer} {output_file}
""".format(
            synthesizer = ctx.executable.synthesizer.path,
            output_file = stack_zip.path,
            execution_requirements = {
                "no-sandbox": "1",
                "local": "1",
                "no-cache": "1",
                "no-remote": "1",
            },
        ),
        inputs = all_inputs,
        input_manifests = tool_input_manifests,
        progress_message = "Exporting database schema",
        outputs = outputs,
    )

    return [
        DefaultInfo(
            files = depset(outputs),
        ),
    ]

aws_stack_zip_rule = rule(
    implementation = _aws_stack_zip_impl,
    attrs = {"synthesizer": attr.label(executable = True, cfg = "exec")},
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
)

def aws_stack_zip(name, synthesizer):
    aws_stack_zip_rule(
        name = name,
        synthesizer = synthesizer,
        tags = [
            "no-remote",
            "no-sandbox",
            "requires-network",
        ],
    )

def aws(name, deps = None, visibility = None):
    aws_cdk_synthesizer(name = name + "_synthesizer", deps = deps, visibility = visibility)

    aws_stack_zip(
        name = name + "_stack_zip",
        synthesizer = ":" + name + "_synthesizer",
    )

    cdk.cdk_binary(
        name = name,
        data = [
            ":" + name + "_synthesizer_deploy.jar",
            ":" + name + "_stack_zip",
        ],
        expand_args = True,
        chdir = native.package_name(),
        fixed_args = [
            "--app",
            "$$(rm -rf stack; mkdir stack; cd stack; unzip -qq ../stack.zip; echo stack)",
        ],
        tags = ["no-remote", "no-sandbox", "requires-network", "manual"],
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
            "manual",
        ],
        tool = ":" + name,
        visibility = visibility,
    )
