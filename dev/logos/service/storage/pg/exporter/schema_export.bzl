load("@aspect_rules_js//js/private:js_info.bzl", "js_info")
load("@aspect_rules_js//js:defs.bzl", "js_library")

def _schema_export_impl(ctx):
    schema_export_json = ctx.actions.declare_file("schema_export.json")
    outputs = [schema_export_json]

    tool_inputs, tool_input_manifests = ctx.resolve_tools(tools = [ctx.attr.exporter])

    ctx.actions.run_shell(
        # required to pass db connection params to exporter's DatabaseModule
        use_default_shell_env = True,
        command = """{exporter} -- json {bin_dir} {output_package} '{tables}'
""".format(
            bin_dir = ctx.bin_dir.path,
            exporter = ctx.executable.exporter.path,
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
        "exporter": attr.label(executable = True, cfg = "exec"),
        "migrations": attr.label_list(allow_files = False),
        "deps": attr.label_list(allow_files = True),
        "tables": attr.string_list_dict(mandatory = True),
    },
    toolchains = [
        "@bazel_tools//tools/jdk:toolchain_type",
    ],
)

def schema_export(name, tables, visibility = None, mappers = None, migrations = None):
    native.java_binary(
        name = name + "_exporter",
        main_class = "dev.logos.service.storage.pg.exporter.Exporter",
        runtime_deps = (mappers or []) + ["@logos//dev/logos/service/storage/pg/exporter"],
    )

    schema_export_rule(
        name = name,
        tables = tables,
        tags = [
            "external",
            "no-remote",
            "requires-network",
        ],
        exporter = ":" + name + "_exporter",
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
{exporter} -- proto {bin_dir} {output_package} "$(cat {schema_export_json})"
""".format(
            bin_dir = ctx.bin_dir.path,
            exporter = ctx.executable.exporter.path,
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
            default = Label("//dev/logos/service/storage/pg/exporter"),
            executable = True,
            cfg = "exec",
        ),
        "schema_export": attr.label(allow_single_file = True),
        "tables": attr.string_list_dict(mandatory = True),
    },
    toolchains = [
        "@bazel_tools//tools/jdk:toolchain_type",
    ],
)

def schema_proto_src(name, schema_export, tables, mappers = None, visibility = None):
    native.java_binary(
        name = name + "_exporter",
        main_class = "dev.logos.service.storage.pg.exporter.Exporter",
        runtime_deps = (mappers or []) + ["@logos//dev/logos/service/storage/pg/exporter"],
    )

    schema_proto_src_rule(
        name = name,
        exporter = ":" + name + "_exporter",
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
            outputs.append(ctx.actions.declare_file("%s/%s/%sTableStorage.java" % (schema, table, table_title_case)))
            outputs.append(ctx.actions.declare_file("%s/%sStorageServiceBase.java" % (schema, table_title_case)))

    tool_inputs, tool_input_manifests = ctx.resolve_tools(tools = [ctx.attr.exporter, ctx.attr.schema_export])

    ctx.actions.run_shell(
        # required to pass db connection params to exporter's DatabaseModule
        use_default_shell_env = True,
        command = """
set -e
shopt -s globstar extglob

{exporter} -- java {bin_dir} {output_package} "$(cat {schema_export_json})"
""".format(
            bin_dir = ctx.bin_dir.path,
            exporter = ctx.executable.exporter.path,
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
            executable = True,
            cfg = "exec",
        ),
        "schema_export": attr.label(allow_single_file = True),
        "tables": attr.string_list_dict(mandatory = True),
    },
    toolchains = [
        "@bazel_tools//tools/jdk:toolchain_type",
    ],
)

def java_storage_service(name, schema_export, tables, deps, mappers = None, visibility = None):
    native.java_binary(
        name = name + "_exporter",
        main_class = "dev.logos.service.storage.pg.exporter.Exporter",
        runtime_deps = (mappers or []) + [
            "@logos//dev/logos/service/storage/pg/exporter",
        ],
    )

    java_storage_service_rule(
        name = name + "_src",
        exporter = ":" + name + "_exporter",
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
            "@com_google_protobuf//:protobuf_java",
            "@logos//dev/logos/app",
            "@logos//dev/logos/app/register:module_library",
            "@logos//dev/logos/service/storage/exceptions",
            "@logos//dev/logos/service/storage/pg",
            "@logos//dev/logos/service/storage/pg/exporter/descriptor",
            "@logos//dev/logos/service/storage/validator",
            "@logos//dev/logos/service/storage:storage_library",
            "@logos//dev/logos/auth/user",
            "@maven_logos//:com_google_inject_guice",
            "@maven_logos//:io_grpc_grpc_api",
            "@maven_logos//:io_grpc_grpc_protobuf",
            "@maven_logos//:io_grpc_grpc_stub",
            "@maven_logos//:io_vavr_vavr",
            "@maven_logos//:javax_annotation_javax_annotation_api",
            "@maven_logos//:org_jdbi_jdbi3_core",
        ],
        plugins = [
            "@logos//dev/logos/app/register:module",
        ],
    )
