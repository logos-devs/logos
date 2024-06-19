load("@rules_foreign_cc//foreign_cc:defs.bzl", "configure_make")

filegroup(
    name = "busybox_files",
    srcs = glob([
        "busybox_config.sh",
        "**",
    ]),
)

configure_make(
    name = "busybox",
    args = ["-C busybox"],
    configure_command = "busybox_config.sh",
    configure_in_place = True,
    copts = ["-static"],
    env = {
        "HOSTCC": "clang",
        "HOSTCXX": "clang++",
    },
    lib_source = ":busybox_files",
    out_binaries = ["../_install/bin/busybox"],
)
