load("@aspect_rules_ts//ts:defs.bzl", "ts_project")
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
