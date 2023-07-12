load("@aspect_rules_js//js/private:js_info.bzl", "js_info")

def _schema_export_impl(ctx):
    outputs = [
        ctx.actions.declare_file("export.srcjar"),
    ]
    js_outputs = []
    decl_outputs = []

    bin_dir = ctx.bin_dir.path

    for schema in ctx.attr.tables.keys():
        for table in ctx.attr.tables[schema]:
            proto_js = ctx.actions.declare_file("%s/%s_pb.js" % (schema, table))
            proto_decl = ctx.actions.declare_file("%s/%s_pb.d.ts" % (schema, table))
            grpc_js = ctx.actions.declare_file("%s/%s_grpc_web_pb.js" % (schema, table))
            grpc_decl = ctx.actions.declare_file("%s/%s_grpc_web_pb.d.ts" % (schema, table))
            outputs.extend([proto_js, proto_decl, grpc_js, grpc_decl])
            js_outputs.extend([proto_js, grpc_js])
            decl_outputs.extend([proto_decl, grpc_decl])

    tool_inputs, tool_input_manifests = ctx.resolve_tools(tools = [
        ctx.attr.env,  # $1
        ctx.attr.exporter,  # $2
        ctx.attr.protoc,  # $3
        ctx.attr.protoc_js_plugin,  # $4
        ctx.attr.protoc_grpc_web_plugin,  # $5
        ctx.attr.protoc_grpc_java_plugin,  # $6
    ])
    output_path = ctx.build_file_path.replace("/BUILD", "")
    output_package = output_path.replace("/", ".")

    ctx.actions.run_shell(
        command = """
set -e
shopt -s globstar

source "$(realpath $1)"
mkdir -p client/java

java -classpath $2 dev.logos.stack.service.storage.exporter.Exporter -- {bin_dir} {output_package} '{tables}'

for desc in **/*.desc
do
  schema="$(basename "$(dirname "$desc")")"
  $3 --plugin=protoc-gen-grpc-java=$6 \
     --plugin=protoc-gen-js=$4 \
     --plugin=protoc-gen-grpc-web=$5 \
     --descriptor_set_in="$desc" \
     --java_out={bin_dir} \
     --grpc-java_out={bin_dir} \
     --js_out=import_style=commonjs:{bin_dir}/{output_path}/$schema \
     --grpc-web_out=import_style=commonjs+dts,mode=grpcwebtext:{bin_dir}/{output_path}/$schema \
    $(basename "$desc" .desc).proto
done

cd {bin_dir}
jar cvf ../../../{java_output_file} **/*.java
cd ../../../
""".format(
            bin_dir = bin_dir,
            output_package = output_package,
            output_path = output_path,
            tables = json.encode(ctx.attr.tables),
            java_output_file = outputs[0].path,
        ),
        arguments = [
            ctx.file.env.path,
            ctx.file.exporter.path,
            ctx.attr.protoc.files_to_run.executable.path,
            ctx.attr.protoc_js_plugin.files_to_run.executable.path,
            ctx.attr.protoc_grpc_web_plugin.files_to_run.executable.path,
            ctx.attr.protoc_grpc_java_plugin.files_to_run.executable.path,
        ],
        inputs = tool_inputs,
        input_manifests = tool_input_manifests,
        progress_message = "Exporting database schema",
        outputs = outputs,
    )

    return [
        DefaultInfo(
            files = depset(outputs),
        ),
        js_info(
            sources = depset(js_outputs),
            declarations = depset(decl_outputs),
        ),
    ]

schema_export_rule = rule(
    implementation = _schema_export_impl,
    attrs = {
        "env": attr.label(default = Label("//:env"), allow_single_file = True),
        "exporter": attr.label(
            default = Label("//dev/logos/stack/service/storage/pg/meta:exporter_deploy.jar"),
            allow_single_file = True,
        ),
        "protoc_js_plugin": attr.label(mandatory = True),
        "protoc_grpc_web_plugin": attr.label(mandatory = True),
        "protoc_grpc_java_plugin": attr.label(mandatory = True),
        "protoc": attr.label(mandatory = True),
        "tables": attr.string_list_dict(mandatory = True),
    },
    toolchains = [
        "@bazel_tools//tools/jdk:toolchain_type",
    ],
)

def schema_export(name, tables, visibility):
    schema_export_rule(
        name = name,
        tables = tables,
        protoc = "@com_google_protobuf//:protoc",
        protoc_grpc_java_plugin = "@io_grpc_grpc_java//compiler:grpc_java_plugin",
        protoc_grpc_web_plugin = "@grpc_web//javascript/net/grpc/web/generator:protoc-gen-grpc-web",
        protoc_js_plugin = "@protobuf_javascript//generator:protoc-gen-js",
        tags = [
            "no-remote",
            "requires-network",
        ],
        visibility = visibility,
    )

def storage_java_proto_library(name, srcs, visibility):
    native.java_library(
        name = name,
        srcs = srcs,
        visibility = visibility,
        deps = [
            "//dev/logos/stack/service/storage/pg",
            "@com_google_protobuf//java/core",
            "@io_grpc_grpc_java//api",
            "@io_grpc_grpc_java//stub",
            "@maven//:com_google_guava_guava",
            "@maven//:com_querydsl_querydsl_core",
            "@maven//:com_querydsl_querydsl_sql",
            "@maven//:io_grpc_grpc_protobuf",
            "@maven//:javax_annotation_javax_annotation_api",
            "@maven//:javax_inject_javax_inject",
        ],
    )

#
#    js_library(
#        name = name + "_grpc_web_client",
#        deps = [name],
#        visibility = visibility,
#    )
