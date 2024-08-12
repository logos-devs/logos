load("@aspect_rules_ts//ts:defs.bzl", "ts_project")
load("@aspect_rules_js//js:defs.bzl", "js_run_binary")
load("@npm//:aws-cdk/package_json.bzl", cdk = "bin")
load("@aspect_bazel_lib//lib:jq.bzl", "jq")
load("//bzl:private/cdk.bzl", "cdk_output_jq_filter")

ts_project(
    name = "app",
    srcs = glob([
        "construct/*.ts",
        "stack/*.ts",
    ]) + [
        "app.ts",
    ],
    declaration = True,
    resolve_json_module = True,
    source_map = True,
    transpiler = "tsc",
    tsconfig = "tsconfig.json",
    deps = [
        "//:node_modules/@aws-quickstart/eks-blueprints",
        "//:node_modules/@types/node",
        "//:node_modules/aws-cdk",
        "//:node_modules/aws-cdk-lib",
        "//:node_modules/aws-sdk",
        "//:node_modules/constructs",
        "//:node_modules/reflect-metadata",
    ],
)

cdk.cdk_binary(
    name = "cdk",
    chdir = package_name(),
    data = [
        "cdk.json",
        ":app",
        "//:node_modules/@aws-quickstart/eks-blueprints",
        "//:node_modules/@types/node",
        "//:node_modules/aws-cdk",
        "//:node_modules/aws-cdk-lib",
        "//:node_modules/aws-sdk",
        "//:node_modules/constructs",
        "//:node_modules/reflect-metadata",
        "//:node_modules/source-map-support",
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

jq(
    name = "cognito_public_host_map",
    srcs = [":cdk_run"],
    out = "cognito_public_host_map.json",
    args = ["--raw-output"],
    filter = '."logos-cognito"."logosCognitoPublicHostMap"',
    visibility = ["//app/summer/web:__subpackages__"],
)

jq(
    name = "cognito_server_config",
    srcs = [":cdk_run"],
    out = "cognito_server_config.json",
    args = ["--raw-output"],
    filter = '."logos-cognito"."logosCognitoServerConfig"',
    visibility = ["//app/auth/module:__pkg__"],
)

jq(
    name = "cognito_hosts",
    srcs = [":cdk_run"],
    out = "cognito_hosts.json",
    args = ["--raw-output"],
    filter = '."logos-cognito"."logosCognitoHosts"',
    visibility = ["//app/auth/module:__pkg__"],
)
