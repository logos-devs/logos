load("//bzl:k8s.bzl", "kubectl")

kubectl(
    name = "ingress",
    manifests = [
        "@cert_manager//file",
        "clusterissuer.yaml",
        "externaldns.yaml",
    ],
    visibility = ["//dev/logos:__subpackages__"],
)
