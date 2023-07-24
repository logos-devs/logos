load("//:gitops.bzl", "k8s_deploy")

k8s_deploy(
    name = "db_ro_service",
    manifests = ["manifests/db_ro_service.yaml"],
    visibility = ["//dev/logos/stack:__subpackages__"],
)

k8s_deploy(
    name = "db_rw_service",
    manifests = ["manifests/db_rw_service.yaml"],
    visibility = ["//dev/logos/stack:__subpackages__"],
)
