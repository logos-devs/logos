load("@aspect_rules_ts//ts:defs.bzl", "ts_project")
load("@aspect_rules_js//js:defs.bzl", "js_run_binary")
load("@npm//:aws-cdk/package_json.bzl", cdk = "bin")

ts_project(
    name = "app",
    srcs = glob([
        "construct/*.ts",
        "stack/*.ts",
    ]) + [
        "app.ts",
    ],
    composite = True,
    tsconfig = "tsconfig.json",
    deps = [
        "//:node_modules",
    ],
)

cdk.cdk_binary(
    name = "cdk",
    chdir = package_name(),
    data = [
        "cdk.json",
        ":app",
        "//:node_modules",
    ],
    tags = ["no-sandbox"],
    visibility = ["//visibility:private"],
)

js_run_binary(
    name = "cdk_deploy",
    outs = ["outputs.json"],
    args = [
        "deploy",
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
