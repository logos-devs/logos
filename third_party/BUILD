load("@rules_foreign_cc//foreign_cc:defs.bzl", "configure_make")

filegroup(
    name = "busybox_files",
    srcs = glob([
        "busybox_config.sh",
        "**",
    ]),
)

configure_make(
    name = "busybox_build",
    args = [
        "-C",
        "busybox",
        "-j",
    ],
    configure_command = "busybox_config.sh",
    configure_in_place = True,
    env = {
        "HOSTCC": "clang",
        "HOSTCXX": "clang++",
    },
    lib_source = ":busybox_files",
    #out_binaries = ["_install/bin/busybox"],
    visibility = ["//dev/logos/author:__pkg__"],
)

genrule(
    name = "busybox",
    srcs = [":busybox_build"],
    outs = ["busybox/busybox"],
    cmd = "cp busybox_build/_install/bin/busybox $(OUTS)",
    visibility = ["//dev/logos/author:__pkg__"],
)
