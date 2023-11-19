load(":gitops.bzl", "CLUSTER", "REGISTRY", "USER")

def _kustomization_tpl(ctx):
    tpl = "resources:\n"

    for manifest in ctx.files.manifests:
        tpl += "- {}\n".format(manifest.short_path)

    tpl += "images:\n"

    for image, repository in ctx.attr.images.items():
        image_digest = image[DefaultInfo].files.to_list()[0]
        tpl += "  - name: {}\n".format(repository)
        tpl += "    newName: {}\n".format(REGISTRY + "/" + repository)  # + ":" + fail(dir(image)))

    return tpl

def _kubectl_impl(ctx):
    executable = ctx.actions.declare_file(ctx.attr.name)

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

    script = """#!/bin/bash -eu
cat <<EOF > kustomization.yaml
resources:
{manifests}
images:
{images}
EOF

find . -iname '*.txt'

{kubectl} kustomize --load_restrictor=LoadRestrictionsNone --cluster={cluster} --user={user} |
{kubectl} --cluster={cluster} --user={user} {action} -f-
""".format(
        manifests = manifests,
        images = images,
        kubectl = ctx.attr.kubectl.files_to_run.executable.short_path,
        cluster = CLUSTER,
        user = USER,
        action = ctx.attr.action,
    )

    ctx.actions.write(
        output = executable,
        content = script,
    )

    return [
        DefaultInfo(
            executable = executable,
            runfiles = ctx.runfiles(files = ctx.files.kubectl + ctx.files.manifests + ctx.files.images),
            files = depset([executable]),
        ),
    ]

kubectl_rule = rule(
    implementation = _kubectl_impl,
    attrs = {
        "action": attr.string(mandatory = True),
        "manifests": attr.label_list(allow_files = True),
        "images": attr.label_keyed_string_dict(allow_empty = True),
        "kubectl": attr.label(
            cfg = "exec",
            executable = True,
            default = Label("//:kubectl"),
        ),
    },
    executable = True,
)

def kubectl(name, manifests, images = None, visibility = None):
    if images == None:
        images = {}

    for action in ["apply", "delete"]:
        kubectl_rule(
            name = "{}.{}".format(name, action),
            action = action,
            manifests = manifests,
            images = images,
            tags = [
                "external",
                "no-remote",
                "no-sandbox",
                "requires-network",
            ],
            visibility = visibility,
        )
