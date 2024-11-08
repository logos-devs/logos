genrule(
    name = "bazel-env",
    outs = ["bazel-env.sh"],
    cmd = """
set +e +u

cat > $@ <<EOF
#!/bin/bash
set +e +u

export LOGOS_AWS_ACCOUNT_ID="$$LOGOS_AWS_ACCOUNT_ID"
export LOGOS_AWS_REGION="$$LOGOS_AWS_REGION"
export LOGOS_AWS_EKS_CLUSTER="$$LOGOS_AWS_EKS_CLUSTER"
export LOGOS_AWS_EKS_USER="$$LOGOS_AWS_EKS_USER"
export LOGOS_AWS_REGISTRY="$$LOGOS_AWS_REGISTRY"
export STORAGE_PG_BACKEND_HOST="$$STORAGE_PG_BACKEND_HOST"

echo "LOGOS_AWS_ACCOUNT_ID=\\$$LOGOS_AWS_ACCOUNT_ID"
echo "LOGOS_AWS_REGION=\\$$LOGOS_AWS_REGION"
echo "LOGOS_AWS_EKS_CLUSTER=\\$$LOGOS_AWS_EKS_CLUSTER"
echo "LOGOS_AWS_EKS_USER=\\$$LOGOS_AWS_EKS_USER"
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
    srcs = select({
        "@platforms//os:osx": [
            "@minikube_osx//file",
        ],
        "@platforms//os:linux": [
            "@minikube_linux//file",
        ],
        "//conditions:default": ["@platforms//:incompatible"],
    }),
    tags = ["no-sandbox"],
)

sh_binary(
    name = "kubectl",
    srcs = select({
        "@platforms//os:osx": [
            "@kubectl_osx//file",
        ],
        "@platforms//os:linux": [
            "@kubectl_linux//file",
        ],
        "//conditions:default": ["@platforms//:incompatible"],
    }),
    tags = ["no-sandbox"],
    visibility = [
        "//visibility:public",
    ],
)

sh_binary(
    name = "protoc",
    srcs = select({
        "@platforms//os:linux": [
            "@protoc_linux//:bin/protoc",
        ],
        "@platforms//os:osx": [
            "@protoc_osx//:bin/protoc",
        ],
        "//conditions:default": ["@platforms//:incompatible"],
    }),
    tags = ["no-sandbox"],
    visibility = [
        "//visibility:public",
    ],
)

sh_binary(
    name = "protoc-gen-js",
    srcs = select({
        "@platforms//os:linux": [
            "@protobuf_javascript_linux//:bin/protoc-gen-js",
        ],
        "@platforms//os:osx": [
            "@protobuf_javascript_osx//:bin/protoc-gen-js",
        ],
        "//conditions:default": ["@platforms//:incompatible"],
    }),
    visibility = [
        "//visibility:public",
    ],
)

sh_binary(
    name = "protoc-gen-grpc-web",
    srcs = select({
        "@platforms//os:linux": [
            "@grpc_web_linux//file",
        ],
        "@platforms//os:osx": [
            "@grpc_web_osx//file",
        ],
        "//conditions:default": ["@platforms//:incompatible"],
    }),
    tags = ["no-sandbox"],
    visibility = [
        "//visibility:public",
    ],
)

sh_binary(
    name = "protoc-gen-jsonschema",
    srcs = select({
        "@platforms//os:linux": [
            "@protoc_gen_jsonschema//:protoc-gen-jsonschema",
        ],
        "//conditions:default": ["@platforms//:incompatible"],
    }),
    tags = ["no-sandbox"],
    visibility = [
        "//visibility:public",
    ],
)
