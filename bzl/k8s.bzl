load(":gitops.bzl", "CLUSTER", "REGISTRY", "USER")

def _kustomization_tpl(ctx):
    tpl = "resources:\n"

    for manifest in ctx.files.manifests:
        tpl += "- {}\n".format(manifest.short_path)

    tpl += "images:\n"

    for image, repository in ctx.attr.images.items():
        image_digest = image[DefaultInfo].files.to_list()[0]
        tpl += "  - name: {}\n".format(repository)
        tpl += "    newName: {}\n".format(REGISTRY + "/" + repository)

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
            REGISTRY + "/" + repository,
            image[DefaultInfo].files.to_list()[0].short_path,
        )
        for image, repository in ctx.attr.images.items()
    ])

    image_pushers = []
    image_pusher_runfiles = []

    for image in ctx.attr.image_pushes:
        image_pushers.append(image[DefaultInfo].files_to_run.executable.short_path)
        runfiles = runfiles.merge(image[DefaultInfo].default_runfiles)

    ctx.actions.write(
        output = executable,
        content = """#!/bin/bash -eu
cat <<EOF > kustomization.yaml
resources:
{manifests}
images:
{images}
EOF

{image_pushers}

{kubectl} kustomize --load_restrictor=LoadRestrictionsNone --cluster={cluster} --user={user} |
{kubectl} --cluster={cluster} --user={user} {action} -f-
""".format(
            manifests = manifests,
            images = images,
            image_pushers = "\n".join(image_pushers),
            kubectl = ctx.attr.kubectl.files_to_run.executable.short_path,
            cluster = CLUSTER,
            user = USER,
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
        "images": attr.label_keyed_string_dict(allow_empty = True),
        "image_pushes": attr.label_list(allow_files = True),
        "kubectl": attr.label(
            cfg = "exec",
            default = Label("//:kubectl"),
            executable = True,
        ),
    },
    executable = True,
)

def kubectl(name, manifests, images = None, image_pushes = None, visibility = None):
    if images == None:
        images = {}

    if image_pushes == None:
        image_pushes = []

    for action in ["apply", "delete"]:
        kubectl_rule(
            name = "{}.{}".format(name, action),
            action = action,
            manifests = manifests,
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
