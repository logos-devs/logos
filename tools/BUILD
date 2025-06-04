genrule(
    name = "bazel-env",
    outs = ["bazel-env.sh"],
    cmd = """
set +e +u

cat > $@ <<EOF
#!/bin/bash
set +e

export LOGOS_AWS_REGION="$$LOGOS_AWS_REGION"
export LOGOS_AWS_REGISTRY="$$LOGOS_AWS_REGISTRY"
export STORAGE_PG_BACKEND_HOST="$$STORAGE_PG_BACKEND_HOST"
export LOGOS_DEV_COGNITO_USERNAME="$$LOGOS_DEV_COGNITO_USERNAME"
export LOGOS_DEV_COGNITO_PASSWORD="$$LOGOS_DEV_COGNITO_PASSWORD"

echo "LOGOS_AWS_REGION=\\$$LOGOS_AWS_REGION" >2
echo "LOGOS_AWS_REGISTRY=\\$$LOGOS_AWS_REGISTRY" >2
echo "STORAGE_PG_BACKEND_HOST=\\$$STORAGE_PG_BACKEND_HOST" >2
echo "LOGOS_DEV_COGNITO_USERNAME="\\$$LOGOS_DEV_COGNITO_USERNAME" >2
echo "LOGOS_DEV_COGNITO_PASSWORD="..." >2

exec \\$$@
EOF
""",
    executable = True,
    visibility = ["//visibility:public"],
)

sh_binary(
    name = "minikube",
    srcs = [
        "@minikube_linux//file",
    ],
    tags = ["no-sandbox"],
)

sh_binary(
    name = "kubectl",
    srcs = [
        "@kubectl_linux//file",
    ],
    tags = ["no-sandbox"],
    visibility = [
        "//visibility:public",
    ],
)

sh_binary(
    name = "protoc",
    srcs = [
        "@protoc_linux//:bin/protoc",
    ],
    tags = ["no-sandbox"],
    visibility = [
        "//visibility:public",
    ],
)

sh_binary(
    name = "protoc-gen-js",
    srcs = [
        "@protobuf_javascript_linux//:bin/protoc-gen-js",
    ],
    visibility = [
        "//visibility:public",
    ],
)

sh_binary(
    name = "protoc-gen-grpc-web",
    srcs = [
        "@grpc_web_linux//file",
    ],
    tags = ["no-sandbox"],
    visibility = [
        "//visibility:public",
    ],
)

sh_binary(
    name = "protoc-gen-jsonschema",
    srcs = [
        "@protoc_gen_jsonschema//:protoc-gen-jsonschema",
    ],
    tags = ["no-sandbox"],
    visibility = [
        "//visibility:public",
    ],
)
