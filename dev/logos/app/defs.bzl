K8S_ACTIONS = ["apply", "delete", "diff", "replace"]

def _app_impl(ctx):
    executable = ctx.actions.declare_file(ctx.attr.name)
    runfiles = ctx.runfiles(files = ctx.files.kubectl + ctx.files.rpc + ctx.files.web)

    volumes_yaml = ""
    if ctx.attr.volumes:
        volumes_yaml = "  volumes:\n"
        for name, size in ctx.attr.volumes.items():
            volumes_yaml += ("    - name: {name}\n" +
                             "      size: {size}\n").format(
                name = name,
                size = size,
            )

    ctx.actions.write(
        output = executable,
        content = """#!/bin/bash -eu

CONSOLE_POD_NAME="$({kubectl} get pods -l app=console -o jsonpath="{{.items[0].metadata.name}}")"

RPC_JAR_HASH="$(shasum -a 256 {rpc} | cut -d' ' -f1)"
RPC_JAR_FILENAME="$(basename {rpc} | sed "s|deploy|$RPC_JAR_HASH|")"

{kubectl} cp --no-preserve "$(realpath {rpc})" "$CONSOLE_POD_NAME:/app/service-jars/$RPC_JAR_FILENAME"

{web_sh}

{kubectl} --cluster="$LOGOS_AWS_EKS_CLUSTER" --user="$LOGOS_AWS_EKS_USER" {action} -f <(cat <<EOF
apiVersion: logos.dev/v1
kind: App
metadata:
  name: {name}
  namespace: default
spec:
  service-jars:
    - "$RPC_JAR_FILENAME"
{web_yaml}
{volumes_yaml}
EOF
)

""".format(
            name = ctx.attr.domain,
            kubectl = ctx.attr.kubectl.files_to_run.executable.short_path,
            action = ctx.attr.action,
            rpc = ctx.files.rpc[0].short_path,
            web_sh = """
WEB_HASHES="$(
    find -H {web_files} -type f | while read -r file
    do
        sha256sum "$file" | cut -d' ' -f1
    done
)"

BUNDLE_HASH=$(echo "$WEB_HASHES" | sort | sha256sum | cut -d' ' -f1)

echo "WEB_HASHES=$WEB_HASHES"
echo "BUNDLE_HASH=$BUNDLE_HASH"

# TODO make this swap atomic
{kubectl} exec "$CONSOLE_POD_NAME" -- rm -rf "/app/web-bundles/web_$BUNDLE_HASH"
{kubectl} cp --no-preserve "$(realpath {web_files})" "$CONSOLE_POD_NAME:/app/web-bundles/web_$BUNDLE_HASH"
""".format(
                web_files = ctx.files.web[0].short_path,
                kubectl = ctx.attr.kubectl.files_to_run.executable.short_path,
            ) if ctx.files.web else "",
            web_yaml = """
  web-bundle: "web_$BUNDLE_HASH"
""" if ctx.files.web else "",
            volumes_yaml = volumes_yaml if ctx.attr.volumes else "",
        ),
    )

    return [
        DefaultInfo(
            executable = executable,
            runfiles = runfiles,
            files = depset([executable]),
        ),
    ]

app_rule = rule(
    implementation = _app_impl,
    attrs = {
        "action": attr.string(mandatory = True, values = K8S_ACTIONS),
        "domain": attr.string(mandatory = True),
        "rpc": attr.label(
            mandatory = True,
            providers = [JavaInfo],
            allow_files = [".jar"],
        ),
        "web": attr.label(
            allow_files = True,
        ),
        "kubectl": attr.label(
            cfg = "exec",
            default = Label("//tools:kubectl"),
            executable = True,
        ),
        "volumes": attr.string_dict(),
    },
    executable = True,
)

def app(name, domain, stack_outputs = None, rpc = None, web = None, volumes = None, visibility = None):
    if rpc:
        # this allows us to put both the stack and the RPCs while bundling the stack outputs with the RPCs when deploying
        wrapped_rpc = name + "_rpc_library"

        native.java_binary(
            name = wrapped_rpc,
            resources = stack_outputs,
            create_executable = False,
            runtime_deps = [rpc],
        )
        rpc = wrapped_rpc

    for action in K8S_ACTIONS:
        app_rule(
            name = name if action == "apply" else "{}.{}".format(name, action),
            action = action,
            domain = domain,
            rpc = (rpc + "_deploy.jar") if rpc else None,
            web = web,
            volumes = volumes,
            tags = [
                "external",
                "no-remote",
                "no-sandbox",
                "requires-network",
            ],
            visibility = visibility,
        )
