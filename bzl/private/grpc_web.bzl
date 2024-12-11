load("@aspect_rules_js//js:defs.bzl", "js_library")
load("@aspect_rules_js//js:providers.bzl", "js_info")

def _grpc_web_client_impl(ctx):
    outs = []
    proto_direct_sources = ctx.attr.proto[ProtoInfo].direct_sources
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

    outputs = [ctx.actions.declare_file(out) for out in outs]
    outdir = ctx.bin_dir.path + "/" + ctx.label.workspace_root + "/" + ctx.build_file_path[:-6]

    descriptors = []
    cmd_inputs = []
    for descriptor_set in ctx.attr.proto[ProtoInfo].transitive_descriptor_sets.to_list():
        descriptors.append(descriptor_set.path)
        cmd_inputs.append(descriptor_set)

    ctx.actions.run_shell(
        command = """
    command -v gfind && find_="gfind" || find_="find"

    mkdir -p {outdir}
    $1 {protos} \
      --descriptor_set_in=<(cat {descriptors}) \
      --plugin=protoc-gen-js=$2 \
      --plugin=protoc-gen-grpc-web=$3 \
      --js_out=import_style=commonjs:{outdir} \
      --grpc-web_out=import_style=commonjs+dts,mode=grpcwebtext:{outdir} \
    && pushd {outdir} \
    && $find_ -type f -mindepth 1 -exec mv {{}} . \\; \
    && $find_ -type d -mindepth 1 -maxdepth 1 -exec rm -r {{}} \\; \
    && popd
""".format(
            protos = " ".join([
                file.path.removeprefix(ctx.bin_dir.path + "/").removeprefix(ctx.label.workspace_root + "/")
                for file in proto_direct_sources
            ]),
            descriptors = " ".join(descriptors),
            outdir = outdir,
        ),
        arguments = [
            ctx.attr.protoc.files_to_run.executable.path,
            ctx.attr.protoc_gen_js.files_to_run.executable.path,
            ctx.attr.protoc_gen_grpc_web.files_to_run.executable.path,
        ],
        progress_message = "Generating grpc-web client.",
        inputs = cmd_inputs,
        tools = [
            ctx.executable.protoc,
            ctx.executable.protoc_gen_js,
            ctx.executable.protoc_gen_grpc_web,
        ],
        outputs = outputs,
    )
    return [
        DefaultInfo(
            files = depset(outputs),
            runfiles = ctx.runfiles(files = outputs),
        ),
        js_info(
            target = ctx.label,
            sources = depset([
                outfile
                for outfile in outputs
                if outfile.extension == "js"
            ]),
            types = depset([
                outfile
                for outfile in outputs
                if outfile.extension == "d.ts"
            ]),
        ),
    ]

grpc_web_client_rule = rule(
    implementation = _grpc_web_client_impl,
    attrs = {
        "proto": attr.label(mandatory = True),
        "protoc": attr.label(default = "@logos//tools:protoc", executable = True, cfg = "exec"),
        "protoc_gen_grpc_web": attr.label(default = "@logos//tools:protoc-gen-grpc-web", executable = True, cfg = "exec"),
        "protoc_gen_js": attr.label(default = "@logos//tools:protoc-gen-js", executable = True, cfg = "exec"),
        "deps": attr.label_list(),
        "grpc": attr.bool(default = True),
        "outs": attr.string_list(),
    },
    output_to_genfiles = True,
)

def grpc_web_client(name, proto, visibility, deps = None):
    grpc_web_client_rule(
        name = "{}_files".format(name),
        proto = proto,
        deps = deps,
        visibility = visibility,
    )

    js_library(
        name = name,
        srcs = [":{}_files".format(name)],
        visibility = visibility,
        deps = [
            "//:node_modules/grpc-web",
            "//:node_modules/google-protobuf",
        ],
    )
