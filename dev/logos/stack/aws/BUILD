load(":aws.bzl", "aws")
load(":flags.bzl", "aws_account_id", "aws_region", "eks_cluster_name")

sh_binary(
    name = "aws",
    srcs = ["aws.sh"],
)

aws(
    name = "synthesizer",
)

java_library(
    name = "environment",
    srcs = ["AwsEnvironment.java"],
    deps = ["//dev/logos/config/infra:infra_config"],
    visibility = ["//visibility:public"],
)

eks_cluster_name(
    name = "cluster_name",
    build_setting_default = "logos-eks-stack-cluster",
    visibility = ["//visibility:public"],
)

aws_account_id(
    name = "account_id",
    build_setting_default = "393745367644",
    visibility = ["//visibility:public"],
)

aws_region(
    name = "region",
    build_setting_default = "us-east-2",
    visibility = ["//visibility:public"],
)
