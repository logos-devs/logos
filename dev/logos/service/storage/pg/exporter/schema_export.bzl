load("@aspect_rules_js//js/private:js_info.bzl", "js_info")
load("@aspect_rules_js//js:defs.bzl", "js_library")

def _schema_export_impl(ctx):
    schema_export_json = ctx.actions.declare_file("schema_export.json")
    outputs = [schema_export_json]

    tool_inputs, tool_input_manifests = ctx.resolve_tools(tools = [ctx.attr.exporter])

    ctx.actions.run_shell(
        # required to pass db connection params to exporter's DatabaseModule
        use_default_shell_env = True,
        command = """java -classpath {exporter} dev.logos.service.storage.pg.exporter.Exporter -- json {bin_dir} {output_package} '{tables}'
""".format(
            bin_dir = ctx.bin_dir.path,
            exporter = ctx.file.exporter.path,
            output_package = ctx.build_file_path.replace("/BUILD", "").replace("/", "."),
            schema_export_json = schema_export_json.path,
            tables = json.encode(ctx.attr.tables),
        ),
        # forces a dependency on migrations
        inputs = depset(transitive = [tool_inputs] + [migration[DefaultInfo].files for migration in ctx.attr.migrations]),
        input_manifests = tool_input_manifests,
        progress_message = "Exporting database schema",
        outputs = outputs,
    )

    return [
        DefaultInfo(
            files = depset(outputs),
        ),
    ]

schema_export_rule = rule(
    implementation = _schema_export_impl,
    attrs = {
        "exporter": attr.label(
            default = Label("//dev/logos/service/storage/pg/exporter:exporter_deploy.jar"),
            allow_single_file = True,
        ),
        "migrations": attr.label_list(allow_files = False),
        "deps": attr.label_list(allow_files = True),
        "tables": attr.string_list_dict(mandatory = True),
    },
    toolchains = [
        "@bazel_tools//tools/jdk:toolchain_type",
    ],
)

def schema_export(name, tables, visibility = None, deps = None, migrations = None):
    schema_export_rule(
        name = name,
        tables = tables,
        tags = [
            "external",
            "no-remote",
            "requires-network",
        ],
        deps = deps,
        migrations = migrations,
        visibility = visibility,
    )

def _schema_storage_proto_impl(ctx):
    outputs = []
    for schema in ctx.attr.tables.keys():
        for table in ctx.attr.tables[schema]:
            outputs.append(ctx.actions.declare_file("%s_%s.proto" % (schema, table)))

    tool_inputs, tool_input_manifests = ctx.resolve_tools(tools = [ctx.attr.exporter, ctx.attr.schema_export])

    ctx.actions.run_shell(
        # required to pass db connection params to exporter's DatabaseModule
        use_default_shell_env = True,
        command = """
set -eu
java -classpath {exporter} dev.logos.service.storage.pg.exporter.Exporter -- proto {bin_dir} {output_package} "$(cat {schema_export_json})"
""".format(
            bin_dir = ctx.bin_dir.path,
            exporter = ctx.file.exporter.path,
            output_package = ctx.build_file_path.replace("/BUILD", "").replace("/", "."),
            schema_export_json = ctx.file.schema_export.path,
        ),
        # forces a dependency on migrations
        inputs = depset(transitive = [tool_inputs]),
        input_manifests = tool_input_manifests,
        progress_message = "Exporting database schema",
        outputs = outputs,
    )

    return [
        DefaultInfo(
            files = depset(outputs),
        ),
    ]

schema_proto_src_rule = rule(
    implementation = _schema_storage_proto_impl,
    attrs = {
        "exporter": attr.label(
            default = Label("//dev/logos/service/storage/pg/exporter:exporter_deploy.jar"),
            allow_single_file = True,
        ),
        "schema_export": attr.label(allow_single_file = True),
        "tables": attr.string_list_dict(mandatory = True),
    },
    toolchains = [
        "@bazel_tools//tools/jdk:toolchain_type",
    ],
)

def schema_proto_src(name, schema_export, tables, visibility = None):
    schema_proto_src_rule(
        name = name,
        schema_export = schema_export,
        tables = tables,
        tags = [
            "external",
            "no-remote",
            "requires-network",
        ],
        visibility = visibility,
    )

def _java_storage_service_impl(ctx):
    outputs = []
    for schema in ctx.attr.tables.keys():
        schema_title_case = schema.replace("_", " ").title().replace(" ", "")
        outputs.append(ctx.actions.declare_file("%s.java" % schema_title_case))

        for table in ctx.attr.tables[schema]:
            table_title_case = table.replace("_", " ").title().replace(" ", "")
            outputs.append(ctx.actions.declare_file("%s.java" % schema_title_case))
            outputs.append(ctx.actions.declare_file("%s/%s/StorageModule.java" % (schema, table)))
            outputs.append(ctx.actions.declare_file("%s/%sStorageServiceBase.java" % (schema, table_title_case)))

    tool_inputs, tool_input_manifests = ctx.resolve_tools(tools = [ctx.attr.exporter, ctx.attr.schema_export])

    ctx.actions.run_shell(
        # required to pass db connection params to exporter's DatabaseModule
        use_default_shell_env = True,
        command = """
set -e
shopt -s globstar extglob

java -classpath {exporter} dev.logos.service.storage.pg.exporter.Exporter -- java {bin_dir} {output_package} "$(cat {schema_export_json})"
""".format(
            bin_dir = ctx.bin_dir.path,
            exporter = ctx.file.exporter.path,
            output_package = ctx.build_file_path.replace("/BUILD", "").replace("/", "."),
            schema_export_json = ctx.file.schema_export.path,
        ),
        # forces a dependency on migrations
        inputs = depset(transitive = [tool_inputs]),
        input_manifests = tool_input_manifests,
        progress_message = "Exporting database schema",
        outputs = outputs,
    )

    return [
        DefaultInfo(
            files = depset(outputs),
        ),
    ]

java_storage_service_rule = rule(
    implementation = _java_storage_service_impl,
    attrs = {
        "exporter": attr.label(
            default = Label("//dev/logos/service/storage/pg/exporter:exporter_deploy.jar"),
            allow_single_file = True,
        ),
        "schema_export": attr.label(allow_single_file = True),
        "tables": attr.string_list_dict(mandatory = True),
    },
    toolchains = [
        "@bazel_tools//tools/jdk:toolchain_type",
    ],
)

def java_storage_service(name, schema_export, tables, deps, visibility = None):
    java_storage_service_rule(
        name = name + "_src",
        schema_export = schema_export,
        tables = tables,
        tags = [
            "external",
            "no-remote",
            "requires-network",
        ],
        visibility = visibility,
    )

    native.java_library(
        name = name,
        srcs = [":%s_src" % name],
        visibility = visibility,
        deps = deps + [
            "@logos//dev/logos/app",
            "@logos//dev/logos/app/register:module_library",
            "@logos//dev/logos/service/storage/pg",
            "@logos//dev/logos/service/storage/validator",
            "@logos//dev/logos/service/storage:storage_library",
            "@logos//dev/logos/user",
            "@maven_logos//:com_google_inject_guice",
            "@com_google_protobuf//:protobuf_java",
            "@maven_logos//:io_grpc_grpc_api",
            "@maven_logos//:io_grpc_grpc_protobuf",
            "@maven_logos//:io_grpc_grpc_stub",
            "@maven_logos//:javax_annotation_javax_annotation_api",
        ],
        plugins = [
            "@logos//dev/logos/app/register:module",
        ],
    )
