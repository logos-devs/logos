load("//bzl:k8s.bzl", "kubectl")

kubectl(
    name = "postgres",
    manifests = ["postgres.yaml"],
    visibility = ["//dev/logos:__subpackages__"],
)
