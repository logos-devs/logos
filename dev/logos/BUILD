load("//bzl:k8s.bzl", "kubectl")

kubectl(
    name = "logos",
    deps = [
        "//dev/logos/app/controller",
        "//dev/logos/ingress",
        "//dev/logos/service/backend",
        "//dev/logos/service/client",
        "//dev/logos/service/envoy",
    ],
)
