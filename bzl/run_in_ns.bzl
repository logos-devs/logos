def _run_in_ns_impl(ctx):
    script = ctx.actions.declare_file(ctx.attr.name + "_run_in_ns.sh")

    ctx.actions.write(script, """#!/bin/sh -eux
CHROOT_DIR="$1"; shift 1

mkdir -p "$CHROOT_DIR/dev"
tar -xf $(dirname "$0")/busybox.tar -C "$CHROOT_DIR"

exec unshare --user --mount --pid --fork --root "$CHROOT_DIR" /usr/bin/busybox sh -c "{cmd}"
""".format(
        root_tar = ctx.file.root.path,
        cmd = ctx.attr.cmd,
    ), is_executable = True)

    return [DefaultInfo(
        executable = script,
        runfiles = ctx.runfiles(files = ctx.files.root),
    )]

run_in_ns = rule(
    implementation = _run_in_ns_impl,
    attrs = {
        "root": attr.label(mandatory = True, allow_single_file = True),
        "cmd": attr.string(mandatory = True),
    },
    executable = True,
)
