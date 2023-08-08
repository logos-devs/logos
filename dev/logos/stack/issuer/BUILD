load("//:gitops.bzl", "k8s_deploy")

k8s_deploy(
    name = "issuer",
    manifests = [":issuer_letsencrypt.yaml"],
    visibility = ["//dev/logos/stack:__subpackages__"],
)
