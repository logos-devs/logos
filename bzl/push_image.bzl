load("@rules_oci//oci:defs.bzl", "oci_load", "oci_push")
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
    push_target = name + "_push_impl"
    load_target = name + "_load_impl"
    tags = remote_tags or ["latest"]

    repository_file(
        name = name + "_repository",
        repository = repository,
        visibility = visibility,
    )

    oci_push(
        name = push_target,
        image = image,
        remote_tags = remote_tags,
        repository_file = ":{}_repository".format(name),
        visibility = visibility,
    )

    oci_load(
        name = load_target,
        image = image,
        repo_tags = ["{}:{}".format(repository, tag) for tag in tags],
        visibility = visibility,
    )

    native.alias(
        name = name,
        actual = select({
            "//dev/logos/config/infra:minikube": ":{}".format(load_target),
            "//conditions:default": ":{}".format(push_target),
        }),
        visibility = visibility,
    )
