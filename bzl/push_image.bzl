load("@rules_oci//oci:defs.bzl", "oci_push")

def push_image(name, image, remote_tags, repository):
    native.genrule(
        name = name + "_repository",
        outs = ["repository.txt"],
        cmd = 'echo "$$LOGOS_AWS_REGISTRY/{}" > $@'.format(repository),
    )

    oci_push(
        name = name,
        image = image,
        remote_tags = remote_tags,
        repository_file = ":{}_repository".format(name),
    )
