load("@rules_oci//oci:defs.bzl", "oci_push")

def push_image(name, image, repository, remote_tags = None, visibility = None):
    native.genrule(
        name = name + "_repository",
        outs = [name + "_repository.txt"],
        cmd = 'echo "$$LOGOS_AWS_REGISTRY/{}" > $@'.format(repository),
        visibility = visibility,
    )

    oci_push(
        name = name,
        image = image,
        remote_tags = remote_tags,
        repository_file = ":{}_repository".format(name),
        visibility = visibility,
    )
