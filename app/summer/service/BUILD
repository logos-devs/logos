load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")

java_library(
    name = "service",
    srcs = [
        "EntryStorageService.java",
        "SourceRssStorageService.java",
    ],
    visibility = [
        "//app/summer/module:__subpackages__",
        "//dev/logos/stack:__subpackages__",
    ],
    deps = [
        "//app/summer/storage",
        "//app/summer/storage:storage_proto",
        "//dev/logos/stack/service/storage:storage_library",
        "//dev/logos/stack/service/storage/pg",
        "@com_google_protobuf//:protobuf_java",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//stub",
        "@maven//:com_google_inject_guice",
    ],
)
