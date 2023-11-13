load("//bzl:k8s.bzl", "kubectl")

kubectl(
    name = "nginx",
    manifests = [":ingress.yaml"],
    visibility = ["//dev/logos/stack:__subpackages__"],
)
