load("//bzl:k8s.bzl", "kubectl")

kubectl(
    name = "logos",
    deps = [
        "//dev/logos/app/controller",
        "//dev/logos/gateway",
        "//dev/logos/service/client",
    ],
)
