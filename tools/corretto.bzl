load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file")
load("@rules_java//toolchains:remote_java_repository.bzl", "remote_java_repository")

JDK_URLS = ["https://corretto.aws/downloads/latest/amazon-corretto-23-x64-linux-jdk.tar.gz"]
JDK_PREFIX = "amazon-corretto-23.0.2.7.1-linux-x64"
JDK_SHA256 = "0370c1b48048e55619eff9beace91077da9ece35d4a57d0c5513cd546341c09c"

def _impl(ctx):
    http_file(
        name = "corretto23_jdk_tar",
        downloaded_file_path = "corretto_23.tar.gz",
        sha256 = JDK_SHA256,
        urls = JDK_URLS,
    )

    remote_java_repository(
        name = "corretto23",
        urls = JDK_URLS,
        target_compatible_with = [
            "@platforms//os:linux",
            "@platforms//cpu:x86_64",
        ],
        version = "23",
        strip_prefix = JDK_PREFIX,
        sha256 = JDK_SHA256,
    )

java_repos = module_extension(implementation = _impl)
