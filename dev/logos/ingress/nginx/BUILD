load("//bzl:k8s.bzl", "kubectl")

kubectl(
    name = "nginx",
    manifests = [
        "@cert_manager//file",
        "clusterissuer.yaml",
        "externaldns.yaml",
        "ingress.yaml",
    ],
    visibility = ["//dev/logos/service:__subpackages__"],
    deps = [
        "//dev/logos/service/client",
        "//dev/logos/service/envoy",
    ],
)
