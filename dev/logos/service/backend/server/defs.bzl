load("@aspect_bazel_lib//lib:tar.bzl", "mtree_mutate", "mtree_spec", "tar")
load("@rules_oci//oci:defs.bzl", "oci_image")
load("@logos//tools:corretto.bzl", "JDK_PREFIX")

def java_server_binary(name, deps, resources = None, visibility = None):
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

def java_container(name, base, entrypoint_jar, visibility = None):
    tar(
        name = name + "_entrypoint_tar",
        srcs = [entrypoint_jar],
        mtree = ["entrypoint_deploy.jar uid=0 gid=0 mode=0444 type=file content=$(location " + entrypoint_jar + ")"],
    )

    mtree_spec(
        name = name + "_jdk_mtree",
        srcs = ["@corretto23_jdk"],
    )

    mtree_mutate(
        name = name + "_jdk_opt_mtree",
        mtree = ":" + name + "_jdk_mtree",
        package_dir = "opt/jdk",
    )

    tar(
        name = name + "_jdk",
        srcs = ["@corretto23_jdk"],
        mtree = ":" + name + "_jdk_opt_mtree",
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
            name + "_entrypoint_tar",
            name + "_jdk",
            "@logos//dev/logos/stack/aws/container/cert_bundle_layer:cert_bundle_layer",
        ],
        visibility = visibility,
    )
