load("@rules_oci//oci:defs.bzl", "oci_push")
load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")

def _repository_file_impl(ctx):
    prefix = ctx.attr._registry_prefix[BuildSettingInfo].value
    if prefix and not prefix.endswith("/"):
        prefix = prefix + "/"
    ctx.actions.write(ctx.outputs.out, prefix + ctx.attr.repository)

repository_file = rule(
    implementation = _repository_file_impl,
    attrs = {
        "repository": attr.string(mandatory = True),
        "_registry_prefix": attr.label(
            default = Label("//dev/logos/config/registry:prefix"),
        ),
    },
    outputs = {"out": "%{name}.txt"},
)

def push_image(name, image, repository, remote_tags = None, visibility = None):
    repository_file(
        name = name + "_repository",
        repository = repository,
        visibility = visibility,
    )

    oci_push(
        name = name,
        image = image,
        remote_tags = remote_tags,
        repository_file = ":{}_repository".format(name),
        visibility = visibility,
    )
