load("@build_bazel_rules_nodejs//:providers.bzl", "DeclarationInfo", "JSEcmaScriptModuleInfo", "JSNamedModuleInfo", "declaration_info")
load("@rules_proto//proto:defs.bzl", "ProtoInfo")
load("@aspect_rules_js//js:defs.bzl", "js_library")

def _grpc_web_client_impl(ctx):
    tool_inputs, tool_input_manifests = ctx.resolve_tools(tools = [
        ctx.attr.protoc,
        ctx.attr.protoc_gen_js,
        ctx.attr.protoc_gen_grpc_web,
    ])

    proto_direct_sources = ctx.attr.proto[ProtoInfo].direct_sources
    outs = []
    for direct_source in proto_direct_sources:
        proto_basename = direct_source.basename[:-6]  # removes extension .proto
        outs += [
            "{}_pb.d.ts".format(proto_basename),
            "{}_pb.js".format(proto_basename),
        ]
        if ctx.attr.grpc:
            outs += [
                "{}_grpc_web_pb.d.ts".format(proto_basename),
                "{}_grpc_web_pb.js".format(proto_basename),
            ]

    outputs = [
        ctx.actions.declare_file(f)
        for f in outs
    ]
    outdir = ctx.genfiles_dir.path + "/" + ctx.build_file_path[:-6]
    ctx.actions.run_shell(
        command = """
    command -v gfind && find_="gfind" || find_="find"

    $1 {protos} \
      --descriptor_set_in={input_proto_descriptor} \
      --plugin=protoc-gen-js=$2 \
      --plugin=protoc-gen-grpc-web=$3 \
      --js_out=import_style=commonjs:{outdir} \
      --grpc-web_out=import_style=commonjs+dts,mode=grpcwebtext:{outdir} \
    && pushd {outdir} \
    && $find_ -type f -mindepth 1 -exec mv {{}} . \\; \
    && $find_ -type d -mindepth 1 -maxdepth 1 -exec rm -r {{}} \\; \
    && popd \
""".format(
            protos = " ".join([
                file.path
                for file in proto_direct_sources
            ]),
            input_proto_descriptor = ctx.files.proto[0].path,
            outdir = outdir,
        ),
        arguments = [
            ctx.attr.protoc.files_to_run.executable.path,
            ctx.attr.protoc_gen_js.files_to_run.executable.path,
            ctx.attr.protoc_gen_grpc_web.files_to_run.executable.path,
        ],
        progress_message = "Generating grpc-web client.",
        inputs = ctx.files.proto,
        tools = tool_inputs,
        input_manifests = tool_input_manifests,
        outputs = outputs,
    )
    return [
        DefaultInfo(
            files = depset(outputs),
            runfiles = ctx.runfiles(files = outputs),
        ),
        declaration_info(depset([
            outfile
            for outfile in outputs
            if outfile.extension == "ts"
        ])),
    ]

grpc_web_client_rule = rule(
    implementation = _grpc_web_client_impl,
    attrs = {
        "proto": attr.label(mandatory = True),
        "protoc": attr.label(mandatory = True),
        "protoc_gen_grpc_web": attr.label(mandatory = True),
        "protoc_gen_js": attr.label(mandatory = True),
        "grpc": attr.bool(default = True),
        "outs": attr.string_list(),
    },
    output_to_genfiles = True,
)

def grpc_web_client(name, proto, visibility):
    grpc_web_client_rule(
        name = "{}_files".format(name),
        proto = proto,
        protoc = "@com_google_protobuf//:protoc",
        protoc_gen_grpc_web = "@grpc_web//javascript/net/grpc/web/generator:protoc-gen-grpc-web",
        protoc_gen_js = "@protobuf_javascript//generator:protoc-gen-js",
        visibility = visibility,
    )

    js_library(
        name = name,
        srcs = [":{}_files".format(name)],
        visibility = visibility,
    )
