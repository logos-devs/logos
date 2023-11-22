load("//bzl:k8s.bzl", "kubectl")

kubectl(
    name = "nginx",
    manifests = [":ingress.yaml"],
    visibility = ["//dev/logos/stack:__subpackages__"],
    deps = [
        "//dev/logos/stack/service/client",
        "//dev/logos/stack/service/envoy",
    ],
)
