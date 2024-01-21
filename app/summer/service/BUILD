java_library(
    name = "service",
    srcs = [
        "EntryStorageService.java",
        "FeedService.java",
        "SourceRssStorageService.java",
    ],
    visibility = [
        "//app/summer/module:__subpackages__",
        "//dev/logos/service:__subpackages__",
    ],
    deps = [
        "//app/summer/proto",
        "//app/summer/storage",
        "//dev/logos/service/storage:storage_library",
        "//dev/logos/service/storage/pg",
        "@com_google_protobuf//:protobuf_java",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//stub",
        "@maven//:com_google_inject_guice",
    ],
)
