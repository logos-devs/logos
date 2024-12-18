genrule(
    name = "bazel-env",
    outs = ["bazel-env.sh"],
    cmd = """
set +e +u

cat > $@ <<EOF
#!/bin/bash
set +e +u

export LOGOS_AWS_REGION="$$LOGOS_AWS_REGION"
export LOGOS_AWS_REGISTRY="$$LOGOS_AWS_REGISTRY"
export STORAGE_PG_BACKEND_HOST="$$STORAGE_PG_BACKEND_HOST"

echo "LOGOS_AWS_REGION=\\$$LOGOS_AWS_REGION"
echo "LOGOS_AWS_REGISTRY=\\$$LOGOS_AWS_REGISTRY"
echo "STORAGE_PG_BACKEND_HOST=\\$$STORAGE_PG_BACKEND_HOST"

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
