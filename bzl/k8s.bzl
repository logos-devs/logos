def _kustomization_tpl(ctx):
    tpl = "resources:\n"

    for manifest in ctx.files.manifests:
        tpl += "- {}\n".format(manifest.short_path)

    tpl += "images:\n"

    for image, repository in ctx.attr.images.items():
        image_digest = image[DefaultInfo].files.to_list()[0]
        tpl += "  - name: {}\n".format(repository)
        tpl += "    newName: {}\n".format("$LOGOS_AWS_REGISTRY/" + repository)

    return tpl

def _kubectl_impl(ctx):
    executable = ctx.actions.declare_file(ctx.attr.name)
    runfiles = ctx.runfiles(files = ctx.files.kubectl + ctx.files.manifests + ctx.files.images)

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
    image_pusher_runfiles = []

    for image in ctx.attr.image_pushes:
        image_pushers.append(image[DefaultInfo].files_to_run.executable.short_path)
        runfiles = runfiles.merge(image[DefaultInfo].default_runfiles)

    deps = []
    deps_runfiles = []

    if ctx.attr.action == "apply":
        for dep in ctx.attr.deps:
            deps.append(dep[DefaultInfo].files_to_run.executable.short_path)
            runfiles = runfiles.merge(dep[DefaultInfo].default_runfiles)

        for migration in ctx.attr.migrations:
            runfiles = runfiles.merge(ctx.runfiles(files = migration[DefaultInfo].files.to_list()))

    ctx.actions.write(
        output = executable,
        content = """#!/bin/bash -eu
{deps}
{image_pushers}

cat <<EOF > kustomization.yaml
resources:
{manifests}
images:
{images}
EOF

{kubectl} kustomize --load_restrictor=LoadRestrictionsNone --cluster="$LOGOS_AWS_EKS_CLUSTER" --user="$LOGOS_AWS_EKS_USER" |
{kubectl} --cluster="$LOGOS_AWS_EKS_CLUSTER" --user="$LOGOS_AWS_EKS_USER" {action} -f-
""".format(
            manifests = manifests,
            images = images,
            image_pushers = "\n".join(image_pushers),
            deps = "\n".join(deps),
            kubectl = ctx.attr.kubectl.files_to_run.executable.short_path,
            action = ctx.attr.action,
        ),
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
        "manifests": attr.label_list(allow_files = True),
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

def kubectl(name, manifests, migrations = None, deps = None, images = None, image_pushes = None, visibility = None):
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
            migrations = migrations,
            deps = deps,
            images = images,
            image_pushes = image_pushes,
            tags = [
                "external",
                "no-remote",
                "no-sandbox",
                "requires-network",
            ],
            visibility = visibility,
        )
