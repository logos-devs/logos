load("@logos//dev/logos/stack/k8s:defs.bzl", "k8s_manifest")
load("@logos//bzl:push_image.bzl", "push_image")
load("//bzl:k8s.bzl", "kubectl")

K8S_ACTIONS = ["apply", "delete", "diff", "replace"]

def _app_impl(ctx):
    executable = ctx.actions.declare_file(ctx.attr.name)
    runfiles = ctx.runfiles(files = ctx.files.kubectl + ctx.files.web)

    rpc_servers_yaml = ""
    if ctx.attr.rpc_servers:
        rpc_servers_yaml = "  rpc-servers:\n"
        for domain, service_name in ctx.attr.rpc_servers.items():
            rpc_servers_yaml += ("    - domain: {domain}\n" +
                                 "      service-name: {service_name}\n").format(
                domain = domain,
                service_name = service_name,
            )

    deps = []
    if ctx.attr.action == "apply":
        for dep in ctx.attr.deps:
            deps.append(dep[DefaultInfo].files_to_run.executable.short_path)
            runfiles = runfiles.merge(dep[DefaultInfo].default_runfiles)

    ctx.actions.write(
        output = executable,
        content = """#!/bin/bash -eu
CONSOLE_POD_NAME="$({kubectl} get pods -l app=console -o jsonpath="{{.items[0].metadata.name}}")"

forward_local_port() {{
    local pod="$1"
    local local_port="$2"
    local port="$3"
    shift 3

    {kubectl} port-forward "$pod" $local_port:$port &
    PORT_FORWARD_PID=$!
    trap "kill $PORT_FORWARD_PID" EXIT ERR
}}

await_port() {{
    local local_port="$1"; shift 1

    while ! nc -vz localhost $local_port > /dev/null 2>&1 ; do
        sleep 0.1
    done
}}

sync_files() {{
    local src="$1"
    local dest="$2"
    shift 2

    if [ -d /mnt/web-bundles ]; then
        echo "Using local mount for sync"
        dest="/mnt/$dest"
        port=""
    else
        echo "Using port-forward for sync"
        forward_local_port "$CONSOLE_POD_NAME" 11873 873
        port="--port=11873"
        await_port 11873
        dest="localhost::$dest"
    fi

    rsync -av --progress -p --chmod=Fu=rw,Du=rwx,Fg=r,Dg=rx,Fo=r,Do=rx $port "$src" "$dest"
}}

{web_sh}
{deps}

{kubectl} {action} -f <(cat <<EOF
apiVersion: logos.dev/v1
kind: App
metadata:
  name: {name}
  namespace: default
spec:
{web_yaml}
{rpc_servers_yaml}
EOF
)

""".format(
            name = ctx.attr.domain,
            deps = "\n".join(deps),
            kubectl = ctx.attr.kubectl.files_to_run.executable.short_path,
            action = ctx.attr.action,
            web_sh = """
WEB_HASHES="$(
    find -H {web_files} -type f | while read -r file
    do
        sha256sum "$file" | cut -d' ' -f1
    done
)"

BUNDLE_HASH="DEV" #$(echo "$WEB_HASHES" | sort | sha256sum | cut -d' ' -f1)

echo "BUNDLE_HASH=$BUNDLE_HASH"

sync_files "$(realpath {web_files})/" web-bundles/web_$BUNDLE_HASH
""".format(
                web_files = ctx.files.web[0].short_path,
                kubectl = ctx.attr.kubectl.files_to_run.executable.short_path,
            ) if ctx.files.web else "",
            web_yaml = """
  web-bundle: "web_$BUNDLE_HASH"
""" if ctx.files.web else "",
            rpc_servers_yaml = rpc_servers_yaml if ctx.attr.rpc_servers else "",
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
        "web": attr.label(
            allow_files = True,
        ),
        "migrations": attr.label_list(allow_files = False),
        "deps": attr.label_list(allow_files = False),
        "rpc_servers": attr.string_dict(),
        "kubectl": attr.label(
            cfg = "exec",
            default = Label("//tools:kubectl"),
            executable = True,
        ),
    },
    executable = True,
)

def app(
        name,
        domain,
        k8s_stack = None,
        rpc_servers = None,
        rpc_server_image = None,
        sidecar_container_images = None,
        manifests = None,
        migrations = None,
        stack_outputs = None,
        web_client = None,
        visibility = None):
    if migrations == None:
        migrations = []
    if sidecar_container_images == None:
        sidecar_container_images = {}

    image_pushes = []
    if rpc_server_image:
        if ":" not in rpc_server_image:
            rpc_server_image = rpc_server_image + ":" + rpc_server_image.rsplit("/", 1)[1]

        rpc_server_push_name = name + "_rpc_server_image_push"
        push_image(
            name = rpc_server_push_name,
            image = rpc_server_image,
            remote_tags = ["latest"],
            repository = "logos-ecr-backend",
            visibility = visibility,
        )
        image_pushes.append(rpc_server_push_name)

    for i, container_repository in enumerate(sidecar_container_images.keys()):
        sidecar_container_push_name = name + "_sidecar_container_image_push_{}".format(i)
        sidecar_container_image = sidecar_container_images[container_repository]
        push_image(
            name = sidecar_container_push_name,
            image = sidecar_container_image,
            repository = container_repository,
            visibility = visibility,
        )
        image_pushes.append(sidecar_container_push_name)

    k8s_manifest(
        name = name + "_k8s_manifest",
        deps = [k8s_stack],
        visibility = visibility,
    )

    images = {}
    if rpc_server_image:
        images[rpc_server_image + ".digest"] = "logos-ecr-backend"

    for container_repository, sidecar_container_image in sidecar_container_images.items():
        if ":" not in sidecar_container_image:
            sidecar_container_image = sidecar_container_image + ":" + sidecar_container_image.rsplit("/", 1)[1]

        images[sidecar_container_image + ".digest"] = container_repository

    kubectl(
        name = name + "_kubectl",
        image_pushes = image_pushes,
        images = images,
        manifests = [name + "_k8s_manifest"],
        migrations = migrations,
        visibility = visibility,
    )

    for action in K8S_ACTIONS:
        app_rule(
            name = name if action == "apply" else "{}.{}".format(name, action),
            action = action,
            domain = domain,
            rpc_servers = rpc_servers,
            deps = [":" + name + "_kubectl"] if action == "apply" else [],
            web = web_client,
            tags = [
                "external",
                "no-remote",
                "no-sandbox",
                "requires-network",
            ],
            visibility = visibility,
        )
