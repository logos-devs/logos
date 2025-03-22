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
import commonjs from '@rollup/plugin-commonjs';
import includePaths from 'rollup-plugin-includepaths';
import json from '@rollup/plugin-json';
import html from '@rollup/plugin-html';
import {makeHtmlAttributes} from '@rollup/plugin-html';
import postcss from 'rollup-plugin-postcss';
import postcssImport from 'postcss-import';
import replace from '@rollup/plugin-replace';
import nodeResolve from '@rollup/plugin-node-resolve';
import sourcemaps from 'rollup-plugin-sourcemaps2';
import {typescriptPaths} from 'rollup-plugin-typescript-paths';
import execute from "rollup-plugin-shell";
import { createRequire } from 'node:module';
import path from 'node:path';

const require = createRequire(import.meta.url);

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
        postcss({
          plugins: [
            postcssImport({
              resolve: (id, basedir) => {
                // Pass through external URLs (CDNs)
                if (id.startsWith('http://') || id.startsWith('https://')) {
                  return id;
                }
                // Handle module imports (e.g., @adobe/spectrum-web-components/dist/...)
                if (!id.startsWith('.') && id.match(/\\.css$$/)) {
                  try {
                    return require.resolve(id);
                  } catch (e) {
                    // Not a module, try relative resolution below
                  }
                }
                // Resolve relative imports (e.g., 'core-global.css') from basedir
                const fullPath = path.resolve(basedir, id);
                try {
                  return require.resolve(fullPath);
                } catch (e) {
                  throw new Error(`Failed to resolve CSS import "$${id}" from "$${basedir}": $${e.message}`);
                }
              },
            })
          ]
        }),
        includePaths({ paths: ["./"] }),
        replace({ preventAssignment: true, values: { 'process.env.NODE_ENV': JSON.stringify('production') }}),
        typescriptPaths(),
        json(),
        sourcemaps(),
        html({
            title: "",
            publicPath: "/",
            async template({attributes, files, meta, publicPath, title}) {
                            const scripts = (files.js || []).slice(0,1)
                                .map(({fileName}) => {
                                    const attrs = makeHtmlAttributes(attributes.script);
                                    return `<script src="$${publicPath}$${fileName}"$${attrs}></script>`;
                                })
                                .join('\\n');

                            const links = (files.css || [])
                                .map(({fileName}) => {
                                    const attrs = makeHtmlAttributes(attributes.link);
                                    return `<link href="$${publicPath}$${fileName}" rel="stylesheet"$${attrs}>`;
                                })
                                .join('\\n');

                            const metas = meta
                                .map((input) => {
                                    const attrs = makeHtmlAttributes(input);
                                    return `<meta$${attrs}>`;
                                })
                                .join('\\n');

                            return `
            <!doctype html>
            <html$${makeHtmlAttributes(attributes.html)}>
              <head>
                <title></title>

                <meta charset="utf-8">
                <meta content="width=device-width, initial-scale=1" name="viewport">
                <meta name="apple-mobile-web-app-capable" content="yes">
                <meta name="apple-mobile-web-app-status-bar-style" content="translucent">
                <meta name="referrer" content="no-referrer">

                $${metas}
                <title>$${title}</title>
                $${links}
              </head>
              <body>
                $${scripts}
              </body>
            </html>`;
            }
        }),
        //terser(),
        execute({
            hook: "buildStart",
            commands: [
                // removes trees coming from external bazel workspaces
                "rm -rf external",
            ],
            sync: true
        }),
        nodeResolve({
            browser: true,
            extensions: [".tsx", ".ts", ".jsx", ".js", ".json"],
        }),
        commonjs({ ignoreDynamicRequires: true }),
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
        sourcemap = "true",
        visibility = ["//visibility:public"],
        deps = deps + [
            "//:node_modules/@rollup/plugin-commonjs",
            "//:node_modules/@rollup/plugin-html",
            "//:node_modules/@rollup/plugin-json",
            "//:node_modules/@rollup/plugin-node-resolve",
            "//:node_modules/@rollup/plugin-replace",
            "//:node_modules/@rollup/plugin-terser",
            "//:node_modules/postcss",
            "//:node_modules/postcss-import",
            "//:node_modules/rollup-plugin-postcss",
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
