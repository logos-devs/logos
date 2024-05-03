load("//bzl:k8s.bzl", "kubectl")

kubectl(
    name = "db_ro_service",
    manifests = ["manifests/db_ro_service.yaml"],
    visibility = ["//dev/logos/service:__subpackages__"],
)

kubectl(
    name = "db_rw_service",
    manifests = ["manifests/db_rw_service.yaml"],
    visibility = [
        "//app/summer/jobs:__subpackages__",
        "//dev/logos/service:__subpackages__",
    ],
    migrations = [
        "//dev/logos/service/storage/migrations",
    ]
)
