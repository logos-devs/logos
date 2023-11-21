load("@rules_oci//oci:defs.bzl", "oci_push")
load(":gitops.bzl", "REGISTRY")

def _push_image_impl(ctx):
    image = ctx.runfiles(files = ctx.files.image)
    inputs = image

    #inputs = image.merge_all(image[DefaultInfo].default_runfiles)
    out = ctx.actions.declare_file("push_image.txt")

    ctx.actions.run_shell(
        outputs = [out],
        inputs = inputs.files,
        command = "$1 > $2 2>&1",
        arguments = [ctx.files.image[0].path, out.path],
        tools = [ctx.attr.yq[DefaultInfo].files_to_run],
    )
    return [
        DefaultInfo(files = depset([out])),
    ]

push_image_rule = rule(
    implementation = _push_image_impl,
    attrs = {
        "image": attr.label(mandatory = True, allow_single_file = True),
        "yq": attr.label(default = "@yq_toolchains//:resolved_toolchain"),
    },
)

def push_image(name, image, remote_tags, repository):
    oci_push(
        name = name + "_oci",
        image = image,
        remote_tags = remote_tags,
        repository = REGISTRY + "/" + repository,
    )

    push_image_rule(
        name = name,
        image = name + "_oci",
    )
