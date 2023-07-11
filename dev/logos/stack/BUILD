load("@com_adobe_rules_gitops//gitops:defs.bzl", "k8s_deploy")
load("//:gitops.bzl", "k8s_deploy")

# exports_files(["eks_production.yaml"])

sh_binary(
    name = "eks_production",
    srcs = select({
        "@platforms//os:osx": [
            "@eksctl_osx//file",
        ],
        "@platforms//os:linux": [
            "@eksctl_linux//:eksctl_linux",
        ],
        "//conditions:default": ["@platforms//:incompatible"],
    }),
    args = [
        "create",
        "cluster",
        "-f",
        "eks_production.yaml",
    ],
    data = ["eks_production.yaml"],
)

k8s_deploy(
    name = "hot",
    objects = [
        "//dev/logos/stack/service/backend:service_hot",
        "//dev/logos/stack/service/client:service_hot",
    ],
)

k8s_deploy(
    name = "stack",
    objects = [
        "//dev/logos/stack/service/backend:backend",
        "//dev/logos/stack/service/client:client",
        "//dev/logos/stack/service/envoy:envoy",
        "//dev/logos/stack/service/storage:storage",
        "//dev/logos/stack/ingress/nginx:nginx",
    ],
)
