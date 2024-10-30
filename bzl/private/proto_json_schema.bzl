def _proto_json_schema_impl(ctx):
    tool_inputs, tool_input_manifests = ctx.resolve_tools(tools = [
        ctx.attr.protoc,
        ctx.attr.protoc_gen_jsonschema,
        ctx.attr.flatten_json_schema,
    ])

    outs = []
    proto_direct_sources = ctx.attr.proto[ProtoInfo].direct_sources
    entrypoints = ctx.attr.entrypoints
    outdir = ctx.bin_dir.path + "/" + ctx.label.workspace_root + "/" + ctx.build_file_path[:-6]

    # Prepare descriptors and inputs
    descriptors = []
    cmd_inputs = []
    for descriptor_set in ctx.attr.proto[ProtoInfo].transitive_descriptor_sets.to_list():
        descriptors.append(descriptor_set.path)
        cmd_inputs.append(descriptor_set)

    outputs = []

    for proto_file in entrypoints:
        for message in entrypoints[proto_file]:
            proto_basename = proto_file[:-6].split("/")[-1]
            raw_out_name = "{proto_basename}_{message}.raw.schema.json".format(proto_basename = proto_basename, message = message)
            final_out_name = "{proto_basename}_{message}.schema.json".format(proto_basename = proto_basename, message = message)
            raw_out_file = ctx.actions.declare_file(raw_out_name)
            final_out_file = ctx.actions.declare_file(final_out_name)
            outs.append(final_out_file)

            # Run protoc with entrypoint for each message in each proto to generate raw schema
            ctx.actions.run_shell(
                command = """
                    command -v gfind && find_="gfind" || find_="find"

                    mkdir -p {outdir}
                    $1 {proto} \
                      --descriptor_set_in=<(cat {descriptors}) \
                      --plugin=protoc-gen-jsonschema=$2 \
                      --jsonschema_opt=output_file_suffix=_{entrypoint}.raw.schema.json \
                      --jsonschema_out={outdir} \
                      --jsonschema_opt=entrypoint_message={entrypoint}

                    pushd {outdir} \
                    && $find_ -type f -mindepth 1 -exec mv {{}} . \\; \
                    && $find_ -type d -mindepth 1 -maxdepth 1 -exec rm -r {{}} \\; \
                    && popd
                """.format(
                    proto = proto_file,
                    descriptors = " ".join(descriptors),
                    entrypoint = message,
                    outdir = outdir,
                ),
                arguments = [
                    ctx.attr.protoc.files_to_run.executable.path,
                    ctx.attr.protoc_gen_jsonschema.files_to_run.executable.path,
                ],
                progress_message = "Generating raw JSON Schema for {proto_file}:{message}".format(proto_file = proto_file, message = message),
                inputs = cmd_inputs,
                tools = tool_inputs,
                input_manifests = tool_input_manifests,
                outputs = [raw_out_file],
            )

            ctx.actions.run_shell(
                command = '%s "$(realpath %s)" "$(realpath %s)"' % (
                    ctx.attr.flatten_json_schema.files_to_run.executable.path,
                    raw_out_file.path,
                    final_out_file.path,
                ),
                progress_message = "Flattening JSON Schema for %s:%s" % (proto_file, message),
                env = {"BAZEL_BINDIR": ctx.bin_dir.path},
                inputs = [raw_out_file],
                outputs = [final_out_file],
                tools = tool_inputs,
                input_manifests = tool_input_manifests,
            )

            outputs.append(final_out_file)

    return [
        DefaultInfo(
            files = depset(outputs),
            runfiles = ctx.runfiles(files = outputs),
        ),
    ]

proto_json_schema_rule = rule(
    implementation = _proto_json_schema_impl,
    attrs = {
        "proto": attr.label(mandatory = True, providers = [ProtoInfo]),
        "protoc": attr.label(default = "@logos//tools:protoc"),
        "protoc_gen_jsonschema": attr.label(default = "@logos//tools:protoc-gen-jsonschema"),
        "flatten_json_schema": attr.label(default = "@logos//tools/flatten-json-schema:flatten-json-schema"),
        "entrypoints": attr.string_list_dict(mandatory = True),
        "deps": attr.label_list(),
    },
    toolchains = ["@aspect_bazel_lib//lib:jq_toolchain_type"],
    output_to_genfiles = True,
)

def proto_json_schema(name, proto, entrypoints, visibility = None, deps = None):
    proto_json_schema_rule(
        name = name,
        proto = proto,
        entrypoints = entrypoints,
        visibility = visibility,
        deps = deps,
    )
