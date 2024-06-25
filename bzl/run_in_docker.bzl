def _run_in_docker_impl(ctx):
    script = ctx.actions.declare_file(ctx.attr.name + "_run_in_docker.sh")

    ctx.actions.write(script, """#!/bin/bash -eu
JOB_NAME="logos"

JOB_DIR="$(mktemp -d "$BUILD_WORKSPACE_DIRECTORY/jobs/${JOB_NAME}_$(date +"%Y%m%d%H%M")_$(pwgen -s -A0 8 1)_XXX")"
JOB_SRC_DIR="$JOB_DIR/src"
JOB_HOSTNAME="$(basename "$JOB_DIR")"
mkdir "$JOB_SRC_DIR"
git worktree add -b "jobs/$JOB_HOSTNAME" "$JOB_SRC_DIR/$JOB_NAME"

docker run \\
  --mount "type=bind,source=$JOB_SRC_DIR,target=/src,readonly=false" \\
  --rm \\
  -ti \\
  --hostname "$JOB_HOSTNAME" \\
  author:latest bash
""", is_executable = True)

    return [DefaultInfo(
        executable = script,
    )]

run_in_docker = rule(
    implementation = _run_in_docker_impl,
    attrs = {},
    executable = True,
)
