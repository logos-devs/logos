load(":gitops.bzl", "CLUSTER", "REGISTRY", "USER")

def _kustomization_tpl(ctx):
    return "resources:\n{manifests}\nimages:\n{images}".format(
        manifests = "\n".join(["- " + f.short_path for f in ctx.files.manifests]),
        images = "\n".join([
            "  - name: {repository}\n    newName: {image_url}".format(
                repository = repository.split(":")[0],
                image_url = REGISTRY + "/" + repository,
            )
            for image, repository in ctx.attr.images.items()
        ]),
    )

def _kubectl_impl(ctx):
    executable = ctx.actions.declare_file(ctx.attr.name)

    for image in ctx.files.images:
        out = ctx.actions.declare_file(image.dirname + "/image_push.txt")
        ctx.actions.run_shell(
            inputs = [image],
            outputs = [out],
            command = image.short_path,
        )

    script = """#!/bin/bash -eu
cat <<EOF > kustomization.yaml
{kustomization}
EOF

{kubectl} kustomize --load_restrictor=LoadRestrictionsNone --cluster={cluster} --user={user} |
{kubectl} --cluster={cluster} --user={user} {action} -f-
""".format(
        kustomization = _kustomization_tpl(ctx),
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
            runfiles = ctx.runfiles(files = ctx.files.kubectl + ctx.files.manifests),
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
