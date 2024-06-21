def _run_in_ns_impl(ctx):
    script = ctx.actions.declare_file(ctx.attr.name + "_run_in_ns.sh")

    ctx.actions.write(script, """#!/bin/bash -eu
JOB_DIR="$BUILD_WORKSPACE_DIRECTORY/jobs/job_$(date +"%Y%m%d%H%M")_$(mktemp -u XXXXXX)"
WORKTREE_DIR="$JOB_DIR/src"

mkdir -p "$WORKTREE_DIR"
tar -xf $(dirname "$0")/busybox.tar -C "$JOB_DIR"

env -i PS1='author@logos: $ ' unshare --user --mount --pid --fork --root "$JOB_DIR" /usr/bin/busybox $@
""".format(
        root_tar = ctx.file.root.path,
    ), is_executable = True)

    return [DefaultInfo(
        executable = script,
        runfiles = ctx.runfiles(files = ctx.files.root),
    )]

run_in_ns = rule(
    implementation = _run_in_ns_impl,
    attrs = {
        "root": attr.label(mandatory = True, allow_single_file = True),
    },
    executable = True,
)
