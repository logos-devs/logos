load("@aspect_bazel_lib//lib:tar.bzl", "mtree_mutate", "mtree_spec", "tar")
load("@logos//tools:corretto.bzl", "JDK_PREFIX")
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

def java_image(name, base, files = None, entrypoint = None, mtree = None, server = None, visibility = None):
    if files == None:
        files = []
    if mtree == None:
        mtree = []

    tar(
        name = name + "_tar",
        srcs = files + ([server + "_deploy.jar"] if server else []),
        mtree = mtree + ([
            "server_deploy.jar uid=0 gid=0 mode=0444 type=file content=$(location " + server + "_deploy.jar)",
        ] if server else []),
        visibility = visibility,
    )

    oci_image(
        name = name,
        base = base,
        entrypoint = entrypoint if entrypoint else [
            "dumb-init",
            "--",
            "/" + JDK_PREFIX + "/bin/java",
            "-jar",
            "/server_deploy.jar",
        ],
        tars = [
            name + "_tar",
            "@jdk_tar//file",
            "@logos//dev/logos/stack/aws/container/cert_bundle_layer:cert_bundle_layer",
        ],
        visibility = visibility,
    )
