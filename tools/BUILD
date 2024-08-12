genrule(
    name = "bazel-env",
    outs = ["bazel-env.sh"],
    cmd = """#!/bin/sh -e
cat > $@ <<EOF
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

alias(
    name = "protoc",
    actual = "@com_google_protobuf//:protoc",
    visibility = [
        "//visibility:public",
    ],
)

sh_binary(
    name = "protoc-gen-js",
    srcs = select({
        "@platforms//os:osx": [
            "@protobuf_javascript_osx//:bin/protoc-gen-js",
        ],
        "@platforms//os:linux": [
            "@protobuf_javascript_linux//:bin/protoc-gen-js",
        ],
        "//conditions:default": ["@platforms//:incompatible"],
    }),
    tags = ["no-sandbox"],
    visibility = [
        "//visibility:public",
    ],
)

alias(
    name = "protoc-gen-grpc-web",
    actual = "@grpc_web//javascript/net/grpc/web/generator:protoc-gen-grpc-web",
    visibility = [
        "//visibility:public",
    ],
)
