load("@aspect_rules_rollup//rollup:defs.bzl", "rollup")
load("@npm//:webpack-cli/package_json.bzl", webpack = "bin")
load("//bzl:ts.bzl", "ts_library")

ts_library(
    name = "web_library",
    srcs = [
        "components/base-router.ts",
        "components/router-path.ts",
        "index.html",
        "index.ts",
        "media.d.ts",
    ],
    visibility = ["//app:__pkg__"],
    deps = [
        "//:node_modules/path-to-regexp",
        "//app/digits/web",
        "//app/review/web",
        "//app/summer/web",
        "//dev/logos/service/client/web/module",
    ],
)

rollup(
    name = "web",
    config_file = ":rollup.config.js",
    entry_point = "index.js",
    node_modules = "//:node_modules",
    output_dir = True,
    sourcemap = "true",
    deps = [
        ":web_library",
        "//:node_modules/@rollup/plugin-commonjs",
        "//:node_modules/@rollup/plugin-json",
        "//:node_modules/@rollup/plugin-node-resolve",
        "//:node_modules/rollup",
        "//:node_modules/rollup-plugin-includepaths",
    ],
)

#
#webpack.webpack_cli_binary(
#    name = "dev_server",
#    args = [
#        "serve",
#        "--hot",
#        "--env",
#        "development",
#        "--config",
#        "webpack.config.js",
#    ],
#    chdir = package_name(),
#    data = [
#        "webpack.config.js",
#        ":web_library",
#        "//:node_modules/html-webpack-plugin",
#        "//:node_modules/tsconfig-paths-webpack-plugin",
#        "//:tsconfig",
#    ],
#    tags = [
#        "ibazel_notify_changes",
#    ],
#    visibility = ["//visibility:private"],
#)
#
#webpack.webpack_cli(
#    name = "web",
#    srcs = [
#        "webpack.config.js",
#        ":web_library",
#        "//:node_modules/html-webpack-plugin",
#        "//:node_modules/tsconfig-paths-webpack-plugin",
#        "//:tsconfig",
#    ],
#    args = [
#        "--env",
#        "production",
#        "--config",
#        "webpack.config.js",
#    ],
#    chdir = package_name(),
#    out_dirs = ["dist"],
#    visibility = ["//dev/logos/service/client:__subpackages__"],
#)
