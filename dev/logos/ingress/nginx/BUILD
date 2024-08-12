load("//bzl:k8s.bzl", "kubectl")

kubectl(
    name = "cert-manager",
    manifests = [
        "@cert_manager//file",
    ],
)

kubectl(
    name = "nginx",
    manifests = [
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
