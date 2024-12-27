load("@aspect_rules_js//js:defs.bzl", "js_run_binary")
load("@npm_logos//:aws-cdk/package_json.bzl", cdk = "bin")

cdk.cdk_binary(
    name = "cdk",
    chdir = package_name(),
    data = [
        "//dev/logos/stack/aws/synthesizer",
    ],
    expand_args = True,
    fixed_args = [
        "--app",
        "$$(realpath ../synthesizer/synthesizer)",
    ],
    tags = ["no-sandbox"],
    visibility = ["//visibility:public"],
)

js_run_binary(
    name = "cdk_run",
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
    tool = ":cdk",
)
