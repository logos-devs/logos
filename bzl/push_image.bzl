load("@rules_oci//oci:defs.bzl", "oci_push")

def push_image(name, image, repository, remote_tags = None, visibility = None):
    native.genrule(
        name = name + "_repository",
        outs = [name + "_repository.txt"],
        # Allow tests to run without LOGOS_AWS_REGISTRY set by using an empty default
        cmd = (
            'LOGOS_AWS_REGISTRY=$${LOGOS_AWS_REGISTRY:-}; ' +
            'echo "$$LOGOS_AWS_REGISTRY/%s" > $@'
        ) % repository,
        visibility = visibility,
    )

    oci_push(
        name = name,
        image = image,
        remote_tags = remote_tags,
        repository_file = ":{}_repository".format(name),
        visibility = visibility,
    )
