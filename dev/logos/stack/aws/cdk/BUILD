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
    tags = ["no-sandbox", "requires-network"],
    visibility = ["//visibility:public"],
)
