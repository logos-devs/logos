"""Helpers for loading container images into a local minikube cluster."""

load("@rules_oci//oci:defs.bzl", "oci_load")


def minikube_load_image(name, image, repository, tag = "latest", visibility = None, **kwargs):
    """Load an OCI image into the minikube container runtime.

    Args:
        name: Name of the Bazel target that will perform the load.
        image: Label of the OCI image target (typically an `oci_image`).
        repository: Repository name to apply when tagging the image inside minikube.
        tag: Image tag to apply (defaults to "latest").
        visibility: Optional visibility for the generated target.
        **kwargs: Additional keyword arguments forwarded to `oci_load`.
    """

    repo_tag = "{}:{}".format(repository, tag)

    oci_load(
        name = name,
        image = image,
        repo_tags = [repo_tag],
        visibility = visibility,
        **kwargs
    )
