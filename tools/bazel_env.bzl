load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")

def _bazel_env_impl(ctx):
    context = ctx.attr._kubectl_context[BuildSettingInfo].value
    registry = ctx.attr._registry_prefix[BuildSettingInfo].value
    aws_region = ctx.attr._aws_region[BuildSettingInfo].value
    load_strategy = ctx.attr._load_strategy[BuildSettingInfo].value
    docker_env_exports = ""
    container_registry = registry

    if load_strategy == "load":
        docker_env_exports = """
if command -v minikube >/dev/null 2>&1; then
  eval "$(minikube docker-env --shell bash)"
  export LOGOS_CONTAINER_REGISTRY="${DOCKER_HOST:-}"
else
  echo "minikube executable not found; required for load strategy" >&2
  exit 1
fi
"""
        if not container_registry:
            container_registry = "${DOCKER_HOST:-}"
    else:
        container_registry = registry

    script = """#!/bin/bash
set +e

export LOGOS_KUBECTL_CONTEXT="{context}"
export LOGOS_CONTAINER_REGISTRY="{container_registry}"

if [ -z "${{LOGOS_AWS_REGION+x}}" ]; then
  export LOGOS_AWS_REGION="{aws_region}"
fi

if [ -n "{registry}" ]; then
  export LOGOS_AWS_REGISTRY="{registry}"
fi

{docker_env_exports}

export STORAGE_PG_BACKEND_HOST="${{STORAGE_PG_BACKEND_HOST:-}}"
export LOGOS_DEV_COGNITO_USERNAME="${{LOGOS_DEV_COGNITO_USERNAME:-}}"
export LOGOS_DEV_COGNITO_PASSWORD="${{LOGOS_DEV_COGNITO_PASSWORD:-}}"

echo "LOGOS_KUBECTL_CONTEXT={context}" >&2
echo "LOGOS_CONTAINER_REGISTRY={container_registry}" >&2
echo "LOGOS_AWS_REGION={aws_region}" >&2
echo "STORAGE_PG_BACKEND_HOST=$STORAGE_PG_BACKEND_HOST" >&2
echo "LOGOS_DEV_COGNITO_USERNAME=$LOGOS_DEV_COGNITO_USERNAME" >&2
echo "LOGOS_DEV_COGNITO_PASSWORD=..." >&2

echo "bazel-env args: $@" >&2

exec "$@"
""".format(
        context = context,
        container_registry = container_registry,
        registry = registry,
        aws_region = aws_region,
        docker_env_exports = docker_env_exports,
    )

    ctx.actions.write(
        output = ctx.outputs.out,
        content = script,
        is_executable = True,
    )

    return [DefaultInfo(executable = ctx.outputs.out, files = depset([ctx.outputs.out]))]

bazel_env = rule(
    implementation = _bazel_env_impl,
    attrs = {
        "_kubectl_context": attr.label(default = Label("//dev/logos/config/kubectl:context")),
        "_registry_prefix": attr.label(default = Label("//dev/logos/config/registry:prefix")),
        "_aws_region": attr.label(default = Label("//dev/logos/stack/aws:region")),
        "_load_strategy": attr.label(default = Label("//dev/logos/config/registry:load_strategy")),
    },
    outputs = {"out": "%{name}.sh"},
    executable = True,
)
