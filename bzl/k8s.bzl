load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")

def _kubectl_impl(ctx):
    executable = ctx.actions.declare_file(ctx.attr.name)
    kubectl_info = ctx.attr.kubectl[DefaultInfo]
    runfiles = ctx.runfiles(files = ctx.files.kubectl)
    runfiles = runfiles.merge(kubectl_info.default_runfiles)
    registry_prefix = ctx.attr._registry_prefix[BuildSettingInfo].value
    if registry_prefix:
        registry_prefix = registry_prefix.rstrip("/") + "/"

    kubectl_cmd = ctx.attr.kubectl.files_to_run.executable.short_path
    kubectl_context = ctx.attr._kubectl_context[BuildSettingInfo].value
    if kubectl_context:
        kubectl_cmd = "{} --context={}".format(kubectl_cmd, kubectl_context)

    load_strategy = ctx.attr._load_strategy[BuildSettingInfo].value
    env_lines = []
    if kubectl_context:
        env_lines.append('export LOGOS_KUBECTL_CONTEXT="{}"'.format(kubectl_context))
    if registry_prefix:
        env_lines.append('export LOGOS_CONTAINER_REGISTRY="{}"'.format(registry_prefix))
    if load_strategy == "load":
        env_lines.append('''if command -v minikube >/dev/null 2>&1; then
  eval "$(minikube docker-env --shell bash)"
else
  echo "minikube executable not found; required for load strategy" >&2
  exit 1
fi''')
    env_block = "\n".join(env_lines)

    if ctx.files.manifests:
        runfiles = runfiles.merge(ctx.runfiles(files = ctx.files.manifests + ctx.files.images))

        manifests = "".join([
            "- {}\n".format(manifest.short_path)
            for manifest in ctx.files.manifests
        ])

        image_entries = []
        for image, repository in ctx.attr.images.items():
            digest_path = image[DefaultInfo].files.to_list()[0].short_path
            new_name = "{}{}@$(cat {})".format(
                registry_prefix,
                repository,
                digest_path,
            )
            image_entries.append(
                "  - name: {}\n    newName: {}\n".format(repository, new_name)
            )
        images = "".join(image_entries)

    image_pushers = []

    for image in ctx.attr.image_pushes:
        image_pushers.append(image[DefaultInfo].files_to_run.executable.short_path)
        runfiles = runfiles.merge(image[DefaultInfo].default_runfiles)

    deps = []
    if ctx.attr.action == "apply":
        for dep in ctx.attr.deps:
            deps.append(dep[DefaultInfo].files_to_run.executable.short_path)
            runfiles = runfiles.merge(dep[DefaultInfo].default_runfiles)

        for migration in ctx.attr.migrations:
            runfiles = runfiles.merge(ctx.runfiles(files = migration[DefaultInfo].files.to_list()))

    content = """#!/bin/bash -eu
{env_block}
{deps}
{image_pushers}
""".format(
        image_pushers = "\n".join(image_pushers),
        deps = "\n".join(deps),
        env_block = env_block,
    )

    if ctx.files.manifests:
        content += """
cat <<EOF > kustomization.yaml
resources:
{manifests}
images:
{images}
EOF

{kubectl} kustomize --load_restrictor=LoadRestrictionsNone | {kubectl} {action} {server_side} -f-
""".format(
            manifests = manifests,
            server_side = "--server-side" if ctx.attr.server_side else "",
            images = images,
            kubectl = kubectl_cmd,
            action = ctx.attr.action,
        )

    ctx.actions.write(
        output = executable,
        content = content,
    )

    return [
        DefaultInfo(
            executable = executable,
            runfiles = runfiles,
            files = depset([executable]),
        ),
    ]

kubectl_rule = rule(
    implementation = _kubectl_impl,
    attrs = {
        "action": attr.string(mandatory = True),
        "server_side": attr.bool(default = False),
        "manifests": attr.label_list(allow_files = True, mandatory = False),
        "migrations": attr.label_list(allow_files = False),
        "deps": attr.label_list(allow_files = False),
        "images": attr.label_keyed_string_dict(allow_empty = True),
        "image_pushes": attr.label_list(allow_files = True),
        "kubectl": attr.label(
            cfg = "exec",
            default = Label("//tools:kubectl"),
            executable = True,
        ),
        "_registry_prefix": attr.label(
            default = Label("//dev/logos/config/registry:prefix"),
        ),
        "_kubectl_context": attr.label(
            default = Label("//dev/logos/config/kubectl:context"),
        ),
        "_load_strategy": attr.label(
            default = Label("//dev/logos/config/registry:load_strategy"),
        ),
    },
    executable = True,
)

def kubectl(name, manifests = None, migrations = None, deps = None, images = None, image_pushes = None, server_side = False, visibility = None):
    if images == None:
        images = {}

    if image_pushes == None:
        image_pushes = []

    if deps == None:
        deps = []

    if migrations == None:
        migrations = []

    for action in ["apply", "delete", "diff", "replace"]:
        kubectl_rule(
            name = name if action == "apply" else "{}.{}".format(name, action),
            action = action,
            manifests = manifests,
            migrations = migrations if action == "apply" else [],
            deps = deps,
            images = images,
            image_pushes = image_pushes,
            server_side = server_side if action in ["apply", "replace", "diff"] else False,
            tags = [
                "external",
                "no-remote",
                "no-sandbox",
                "requires-network",
            ],
            visibility = visibility,
        )
