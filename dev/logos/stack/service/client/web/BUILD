load("@aspect_rules_js//js:defs.bzl", "js_library")
load("@npm//:webpack-cli/package_json.bzl", webpack = "bin")

js_library(
    name = "web_library",
    srcs = [
        "index.html",
    ] + glob(
        [
            "**/*.js",
            "**/*.ts",
            "**/*.d.ts",
            "**/*.jpg",
            "**/*.png",
        ],
    ),
    visibility = ["//app:__pkg__"],
    deps = [
        "//:node_modules",
        "//app/auth/web",
        "//app/digits/web",
        "//app/rep/web",
        "//app/review/web",
        "//app/summer/web",
    ],
)

webpack.webpack_cli_binary(
    name = "dev_server",
    args = [
        "serve",
        "--hot",
        "--env",
        "development",
        "--config",
        "webpack.config.js",
    ],
    chdir = package_name(),
    data = [
        "webpack.config.js",
        ":web_library",
        "//:tsconfig",
    ],
    tags = ["ibazel_notify_changes"],
    visibility = ["//visibility:private"],
)

webpack.webpack_cli(
    name = "web",
    srcs = [
        "webpack.config.js",
        ":web_library",
        "//:tsconfig",
    ],
    args = [
        "--env",
        "production",
        "--config",
        "webpack.config.js",
    ],
    chdir = package_name(),
    out_dirs = ["dist"],
    visibility = ["//dev/logos/stack/service/client:__subpackages__"],
)
