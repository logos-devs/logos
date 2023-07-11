load("//:grpc_web.bzl", "grpc_web_client")

grpc_web_client(
    name = "annotation_grpc_web_client",
    proto = "//app/review:annotation_proto",
    visibility = ["//app/review/web:__subpackages__"],
)

grpc_web_client(
    name = "file_grpc_web_client",
    proto = "//app/review:file_proto",
    visibility = ["//app/review/web:__subpackages__"],
)

grpc_web_client(
    name = "project_grpc_web_client",
    proto = "//app/review:project_proto",
    visibility = ["//app/review/web:__subpackages__"],
)
