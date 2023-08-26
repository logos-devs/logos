load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")

java_library(
    name = "entry_storage_service",
    srcs = ["EntryStorageService.java"],
    visibility = [
        "//app/summer/module:__subpackages__",
        "//dev/logos/stack:__subpackages__",
    ],
    deps = [
        "//app/summer/storage:entry_storage",
        "//app/summer/storage:storage_proto",
        "//dev/logos/stack/service/storage:storage_library",
        "@com_google_protobuf//:protobuf_java",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//stub",
        "@maven//:com_google_inject_guice",
    ],
)
