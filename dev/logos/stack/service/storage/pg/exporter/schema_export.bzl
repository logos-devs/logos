load("@aspect_rules_js//js/private:js_info.bzl", "js_info")

def _schema_export_impl(ctx):
    proto_desc_bundle = ctx.actions.declare_file("bundle.desc")
    outputs = [
        ctx.actions.declare_file("export.srcjar"),
        proto_desc_bundle,
    ]
    proto_src_outputs = []
    js_outputs = []
    decl_outputs = []

    bin_dir = ctx.bin_dir.path

    for schema in ctx.attr.tables.keys():
        for table in ctx.attr.tables[schema]:
            proto_src = ctx.actions.declare_file("%s/%s.proto" % (schema, table))
            proto_js = ctx.actions.declare_file("%s/%s_pb.js" % (schema, table))
            proto_decl = ctx.actions.declare_file("%s/%s_pb.d.ts" % (schema, table))
            grpc_js = ctx.actions.declare_file("%s/%s_grpc_web_pb.js" % (schema, table))
            grpc_decl = ctx.actions.declare_file("%s/%s_grpc_web_pb.d.ts" % (schema, table))
            outputs.extend([proto_src, proto_js, proto_decl, grpc_js, grpc_decl])
            proto_src_outputs.append(proto_src)
            js_outputs.extend([proto_js, grpc_js])
            decl_outputs.extend([proto_decl, grpc_decl])

    tool_inputs, tool_input_manifests = ctx.resolve_tools(tools = [
        ctx.attr.exporter,  # $1
        ctx.attr.protoc,  # $2
        ctx.attr.protoc_js_plugin,  # $3
        ctx.attr.protoc_grpc_web_plugin,  # $4
        ctx.attr.protoc_grpc_java_plugin,  # $5
    ])
    output_path = ctx.build_file_path.replace("/BUILD", "")
    output_package = output_path.replace("/", ".")

    ctx.actions.run_shell(
        # required to pass db connection params to exporter's DatabaseModule
        use_default_shell_env = True,
        command = """
set -e
shopt -s globstar extglob

mkdir -p client/java

java -classpath $1 dev.logos.stack.service.storage.exporter.Exporter -- export {bin_dir} {output_package} '{tables}' {proto_desc_bundle_file}

for desc in **/*.desc
do
  if [[ "$(basename $desc)" = "bundle.desc" ]]
  then
    continue
  fi

  schema="$(basename "$(dirname "$desc")")"
  $2 --plugin=protoc-gen-grpc-java=$5 \
     --plugin=protoc-gen-js=$3 \
     --plugin=protoc-gen-grpc-web=$4 \
     --descriptor_set_in="$desc" \
     --java_out={bin_dir} \
     --grpc-java_out={bin_dir} \
     --js_out=import_style=commonjs:{bin_dir} \
     --grpc-web_out=import_style=commonjs+dts,mode=grpcwebtext:{bin_dir} \
  {output_path}/$schema/$(basename "$desc" .desc).proto
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
            proto_desc_bundle_file = proto_desc_bundle.path,
        ),
        arguments = [
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
        ProtoInfo(
            srcs = proto_src_outputs,
            deps = [],
            descriptor_set = proto_desc_bundle,
        ),
        js_info(
            sources = depset(js_outputs),
            declarations = depset(decl_outputs),
        ),
    ]

schema_export_rule = rule(
    implementation = _schema_export_impl,
    attrs = {
        "exporter": attr.label(
            default = Label("//dev/logos/stack/service/storage/pg/exporter:exporter_deploy.jar"),
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
            "external",
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
            "//dev/logos/stack/service/storage:storage_library",
            "@com_google_protobuf//java/core",
            "@io_grpc_grpc_java//api",
            "@io_grpc_grpc_java//protobuf",
            "@io_grpc_grpc_java//stub",
            "@maven//:com_google_guava_guava",
            "@maven//:com_google_inject_guice",
            "@maven//:com_querydsl_querydsl_core",
            "@maven//:com_querydsl_querydsl_sql",
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