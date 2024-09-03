load("@aspect_rules_js//js:defs.bzl", "js_run_binary")
load("@npm//:aws-cdk/package_json.bzl", cdk = "bin")

java_binary(
    name = "app",
    srcs = [
        "AppId.java",
        "InfrastructureModule.java",
        "stack/EcrModule.java",
        "stack/EksModule.java",
        "stack/RdsModule.java",
        "stack/VpcModule.java",
    ],
    args = ["synth"],
    main_class = "dev.logos.stack.aws.cdk.InfrastructureModule",
    deps = [
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:software_amazon_awscdk_aws_cdk_lib",
        "@maven_logos//:software_amazon_awscdk_cdk_lambda_layer_kubectl_v30",
        "@maven_logos//:software_amazon_awssdk_auth",
        "@maven_logos//:software_amazon_awssdk_iam",
        "@maven_logos//:software_amazon_awssdk_sso",
        "@maven_logos//:software_amazon_awssdk_ssooidc",
        "@maven_logos//:software_amazon_awssdk_sts",
        "@maven_logos//:software_constructs_constructs",
    ],
)

cdk.cdk_binary(
    name = "cdk",
    data = [
        ":app_deploy.jar",
    ],
    expand_args = True,
    fixed_args = [
        "--app",
        "'JAR=\"$(location :app_deploy.jar)\"; java -jar $${JAR#bazel-out/k8-fastbuild/bin/}'"
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
