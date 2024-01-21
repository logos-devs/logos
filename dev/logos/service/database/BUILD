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
        # TODO remove this and instead rewrite the feed job to call RPCs. It shouldn't be calling the database directly.
        "//app/summer/jobs/feeds:__pkg__",
        "//dev/logos/service:__subpackages__",
    ],
)
