load("//bzl:ts.bzl", "ts_library")
load("@aspect_rules_js//js:defs.bzl", "js_binary", "js_library", "js_test")

ts_library(
    name = "flatten_json_schema_library",
    srcs = [":flatten-json-schema.mts"],
)

js_binary(
    name = "flatten-json-schema",
    data = [":flatten_json_schema_library"],
    entry_point = "flatten-json-schema.mjs",
    visibility = ["//visibility:public"],
)
