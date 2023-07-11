load("//:gitops.bzl", "k8s_deploy")

k8s_deploy(
    name = "nginx",
    manifests = [":ingress.yaml"],
    visibility = ["//dev/logos/stack:__subpackages__"],
)
