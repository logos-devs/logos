load("@aspect_bazel_lib//lib:yq.bzl", "yq")
load("//bzl:k8s.bzl", "kubectl")

kubectl(
    name = "envoy_gateway",
    manifests = [
        "@envoy_gateway//file",
    ],
    server_side = True,
    visibility = ["//dev/logos:__subpackages__"],
)

yq(
    name = "cert_manager",
    srcs = ["@cert_manager//file"],
    outs = ["cert_manager.yaml"],
    expression = '(select(documentIndex == 45).spec.template.spec.containers[0].args) |= . + ["--enable-gateway-api"]',
)

kubectl(
    name = "gateway",
    manifests = [
        "envoygateway.yaml",
        ":cert_manager",
        "clusterissuer.yaml",
        "externaldns.yaml",
    ],
    server_side = False,
    visibility = ["//dev/logos:__subpackages__"],
    deps = [":envoy_gateway"],
)
