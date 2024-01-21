load("//bzl:k8s.bzl", "kubectl")

kubectl(
    name = "nginx",
    manifests = [":ingress.yaml"],
    visibility = ["//dev/logos/service:__subpackages__"],
    deps = [
        "//dev/logos/service/client",
        "//dev/logos/service/envoy",
    ],
)
