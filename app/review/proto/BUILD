load("//bzl:grpc_web.bzl", "grpc_web_client")

proto_library(
    name = "annotation_proto",
    srcs = ["annotation.proto"],
    visibility = ["//app/review:__subpackages__"],
)

proto_library(
    name = "file_proto",
    srcs = ["file.proto"],
    visibility = ["//app/review:__subpackages__"],
)

grpc_web_client(
    name = "annotation_grpc_web",
    proto = ":annotation_proto",
    visibility = ["//app/review/web:__subpackages__"],
)

grpc_web_client(
    name = "file_grpc_web",
    proto = ":file_proto",
    visibility = ["//app/review/web:__subpackages__"],
)
