load("@aspect_bazel_lib//lib:tar.bzl", "mtree_mutate", "mtree_spec", "tar")
load("@rules_oci//oci:defs.bzl", "oci_image")
load("@logos//tools:corretto.bzl", "JDK_PREFIX")

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

def java_container(name, base, entrypoint_jar, files = None, mtree = None, visibility = None):
    if files == None:
        files = []
    if mtree == None:
        mtree = []

    tar(
        name = name + "_tar",
        srcs = files + [entrypoint_jar],
        mtree = mtree + [
            "entrypoint_deploy.jar uid=0 gid=0 mode=0444 type=file content=$(location " + entrypoint_jar + ")",
        ],
        visibility = visibility,
    )

    oci_image(
        name = name,
        base = base,
        entrypoint = [
            "/usr/sbin/dumb-init",
            "--",
            "/opt/jdk/bin/java".format(JDK_PREFIX),
            "-jar",
            "/entrypoint_deploy.jar",
        ],
        tars = [
            name + "_tar",
            "@logos//dev/logos/stack/aws/container/cert_bundle_layer:cert_bundle_layer",
        ],
        visibility = visibility,
    )
