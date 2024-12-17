def _kubectl_impl(ctx):
    executable = ctx.actions.declare_file(ctx.attr.name)
    runfiles = ctx.runfiles(files = ctx.files.kubectl)

    if ctx.files.manifests:
        runfiles = runfiles.merge(ctx.runfiles(files = ctx.files.manifests + ctx.files.images))

        manifests = "".join([
            "- {}\n".format(manifest.short_path)
            for manifest in ctx.files.manifests
        ])

        images = "".join([
            "  - name: {}\n".format(repository) +
            "    newName: {}@$(cat {})\n".format(
                "$LOGOS_AWS_REGISTRY/" + repository,
                image[DefaultInfo].files.to_list()[0].short_path,
            )
            for image, repository in ctx.attr.images.items()
        ])

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
{deps}
{image_pushers}
""".format(
        image_pushers = "\n".join(image_pushers),
        deps = "\n".join(deps),
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
            kubectl = ctx.attr.kubectl.files_to_run.executable.short_path,
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
