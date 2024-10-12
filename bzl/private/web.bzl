load("@aspect_rules_js//js:defs.bzl", "js_run_devserver")
load("@aspect_rules_rollup//rollup:defs.bzl", "rollup")
load("@aspect_rules_ts//ts:defs.bzl", "ts_project")
#load("@npm//:@web/dev-server/package_json.bzl", wds = "bin")

def rollup_config_file(name):
    native.genrule(
        name = name,
        outs = ["rollup.config.mjs"],
        cmd = """
cat > $@ << 'EOF'
import terser from '@rollup/plugin-terser';
import css from "rollup-plugin-import-css";
import commonjs from '@rollup/plugin-commonjs';
import includePaths from 'rollup-plugin-includepaths';
import json from '@rollup/plugin-json';
import html from '@rollup/plugin-html';
import replace from '@rollup/plugin-replace';
import nodeResolve from '@rollup/plugin-node-resolve';
import sourcemaps from 'rollup-plugin-sourcemaps2';
import {typescriptPaths} from 'rollup-plugin-typescript-paths';
import execute from "rollup-plugin-shell";

const allowedWarnings = {
    'THIS_IS_UNDEFINED': [
        /.*/
    ],
    'CIRCULAR_DEPENDENCY': [
        /\\/inversify@6.0.2\\//,
    ],
    'EVAL': [
        /\\/google-libphonenumber@3.2.34\\//,
        /\\/google-protobuf@3.21.2\\//,
    ]
};

export default {
    plugins: [
        commonjs(),
        css(),
        nodeResolve({
            browser: true,
            extensions: [".tsx", ".ts", ".jsx", ".js", ".json"],
        }),
        includePaths({
            paths: ["./"],
        }),
        replace({
            preventAssignment: true,
            values: {
                'process.env.NODE_ENV': JSON.stringify('production')
            }
        }),
        terser(),
        typescriptPaths(),
        json(),
        sourcemaps(),
        html({
            title: "",
            publicPath: "/"
        }),
        execute({
            hook: "buildStart",
            commands: [
                // removes trees coming from external bazel workspaces
                "rm -rf external",
            ],
            sync: true
        }),
    ],
    output: {
        format: 'es',
        sourcemap: true,
    },
    onwarn(warning, warn) {
        const allowed = allowedWarnings[warning.code];

        if (allowed && (warning.loc === undefined || allowed.some((regex) => regex.test(warning.loc.file)))) {
            warn(warning);
            return;
        }
        if (warning.loc) {
            console.warn(warning.loc.file);
        }
        throw new Error(warning.code + ' : ' + warning.message);
    }
};
EOF
""",
        visibility = ["//visibility:public"],
    )

def web(name, srcs, tsconfig, rollup_config = None, deps = None):
    deps = deps or []

    ts_project(
        name = name + "_ts",
        srcs = srcs,
        composite = True,
        declaration = True,
        resolve_json_module = True,
        source_map = True,
        transpiler = "tsc",
        tsconfig = tsconfig,
        deps = deps,
    )

    if rollup_config == None:
        rollup_config_label = name + "_rollup_config_file"
        rollup_config_file(rollup_config_label)
        rollup_config = ":" + rollup_config_label

    rollup(
        name = name,
        srcs = [
            ":" + name + "_ts",
            tsconfig,
        ],
        config_file = rollup_config,
        entry_point = "index.js",
        node_modules = "//:node_modules",
        output_dir = True,
        sourcemap = "inline",
        visibility = ["//visibility:public"],
        deps = deps + [
            "//:node_modules/@rollup/plugin-commonjs",
            "//:node_modules/@rollup/plugin-html",
            "//:node_modules/@rollup/plugin-json",
            "//:node_modules/@rollup/plugin-node-resolve",
            "//:node_modules/@rollup/plugin-replace",
            "//:node_modules/@rollup/plugin-terser",
            "//:node_modules/rollup-plugin-import-css",
            "//:node_modules/rollup-plugin-includepaths",
            "//:node_modules/rollup-plugin-shell",
            "//:node_modules/rollup-plugin-sourcemaps2",
            "//:node_modules/rollup-plugin-typescript-paths",
            "//:node_modules/typescript",
        ],
    )

#    wds.wds_binary(
#        name = name + "_wds_tool",
#    )
#
#    js_run_devserver(
#        name = "dev",
#        args = [
#            "--watch",
#            "--debug",
#            "--root-dir",
#            "$(location :" + name + ")",
#            "--app-index",
#            "$(location :" + name + ")/index.html",
#            "--port",
#            "8080",
#            "--hostname",
#            "0.0.0.0",
#        ],
#        data = [":" + name],
#        tags = [
#            "ibazel_notify_changes",
#        ],
#        tool = ":" + name + "_wds_tool",
#    )
