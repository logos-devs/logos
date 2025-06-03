sh_binary(
	name = "bazel-env",
	srcs = ["bazel-env.sh"],
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
