load("@aspect_rules_ts//ts:defs.bzl", "ts_project")

def ts_library(name, srcs, visibility = None, deps = None):
    ts_project(
        name = name,
        srcs = srcs,
        transpiler = "tsc",
        tsconfig = "//:tsconfig",
        visibility = visibility,
        deps = deps,
    )
