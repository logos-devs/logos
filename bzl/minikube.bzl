
def _find_tar(files):
    for f in files:
        if f.basename.endswith(".tar"):
            return f
    if files:
        return files[0]
    fail("No image tarball found")

def _minikube_load_impl(ctx):
    image_files = ctx.attr.image[DefaultInfo].files.to_list()
    image_tar = _find_tar(image_files)
    minikube_ftr = ctx.attr.minikube.files_to_run
    minikube_exe = minikube_ftr.executable.short_path
    script = ctx.actions.declare_file(ctx.label.name + ".sh")
    ctx.actions.write(
        output = script,
        content = """#!/bin/bash
set -eu
{minikube} image load {image_tar}
{minikube} image tag {default_tag} {repository}:{tag}
""".format(
            minikube = minikube_exe,
            image_tar = image_tar.short_path,
            default_tag = ctx.attr.default_tag,
            repository = ctx.attr.repository,
            tag = ctx.attr.tag,
        ),
        is_executable = True,
    )

    runfiles = ctx.runfiles(files = image_files)
    runfiles = runfiles.merge(ctx.attr.minikube[DefaultInfo].default_runfiles)
    return [DefaultInfo(executable = script, runfiles = runfiles)]

minikube_load_image = rule(
    implementation = _minikube_load_impl,
    attrs = {
        "image": attr.label(mandatory = True),
        "repository": attr.string(mandatory = True),
        "default_tag": attr.string(mandatory = True),
        "tag": attr.string(default = "latest"),
        "minikube": attr.label(
            default = Label("//tools:minikube"),
            cfg = "exec",
        ),
    },
)
