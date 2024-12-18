load("@aspect_bazel_lib//lib:tar.bzl", "tar")
load("@rules_oci//oci:defs.bzl", "oci_image")

def java_server(name, deps, resources = None, visibility = None):
    native.java_binary(
        name = name,
        resources = resources,
        visibility = visibility,
        runtime_deps = ["@logos//dev/logos/service/backend/server"] + deps,
        main_class = "dev.logos.service.backend.server.ServerExecutor",
        plugins = [
            "@logos//dev/logos/app/register:module",
        ],
    )

def java_image(name, server, base, files = None, mtree = None, visibility = None):
    if files == None:
        files = []
    if mtree == None:
        mtree = []

    tar(
        name = name + "_tar",
        srcs = files + [server + "_deploy.jar"],
        mtree = mtree + [
            "server_deploy.jar uid=0 gid=0 mode=0444 type=file content=$(location " + server + "_deploy.jar)",
        ],
    )

    oci_image(
        name = name,
        base = base,
        entrypoint = [
            "dumb-init",
            "--",
            "java",
            "-jar",
            "/server_deploy.jar",
        ],
        tars = [
            name + "_tar",
            "@logos//dev/logos/stack/aws/container/cert_bundle_layer:cert_bundle_layer",
        ],
    )
