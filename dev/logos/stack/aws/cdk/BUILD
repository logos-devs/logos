load("@aspect_rules_js//js:defs.bzl", "js_run_binary")
load("@npm_logos//:aws-cdk/package_json.bzl", cdk = "bin")

cdk.cdk_binary(
    name = "cdk",
    data = [
        "//dev/logos/stack/aws/synthesizer:synthesizer_deploy.jar",
    ],
    expand_args = True,
    fixed_args = [
        "--app",
        "'JAR=\"$(location //dev/logos/stack/aws/synthesizer:synthesizer_deploy.jar)\"; java -jar $${JAR#bazel-out/k8-fastbuild/bin/}'",
    ],
    tags = ["no-sandbox"],
    visibility = ["//visibility:public"],
)

js_run_binary(
    name = "cdk_run",
    outs = ["outputs.json"],
    args = [
        "deploy",
        "-v",
        "--all",
        "--outputs-file",
        "outputs.json",
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
    tool = ":cdk",
)
