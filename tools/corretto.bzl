load("@rules_java//toolchains:remote_java_repository.bzl", "remote_java_repository")

def _impl(ctx):
    remote_java_repository(
        name = "corretto23",
        urls = ["https://corretto.aws/downloads/latest/amazon-corretto-23-x64-linux-jdk.tar.gz"],
        target_compatible_with = [
            "@platforms//os:linux",
            "@platforms//cpu:x86_64",
        ],
        version = "23",
        strip_prefix = "amazon-corretto-23.0.2.7.1-linux-x64",
        sha256 = "0370c1b48048e55619eff9beace91077da9ece35d4a57d0c5513cd546341c09c",
    )

java_repos = module_extension(implementation = _impl)
