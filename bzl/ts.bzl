load("@aspect_rules_ts//ts:defs.bzl", "ts_project")

def ts_library(name, srcs, visibility = None, deps = None):
    deps = deps or []

    ts_project(
        name = name,
        srcs = srcs,
        transpiler = "tsc",
        tsconfig = "//:tsconfig",
        visibility = visibility,
        resolve_json_module = True,
        source_map = True,
        declaration = True,
        composite = True,
        deps = deps + [
            "//:node_modules/@types/node",
            "//:node_modules/reflect-metadata",
        ],
    )
