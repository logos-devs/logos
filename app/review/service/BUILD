java_library(
    name = "service",
    srcs = [
        "FileService.java",
        "ProjectService.java",
    ],
    visibility = ["//dev/logos/service:__subpackages__"],
    deps = [
        "//app/review/grammar:aipname_parser_java",
        "//app/review/grammar:java9_parser_java",
        "//app/review/proto:annotation_java_grpc",
        "//app/review/proto:file_java_grpc",
        "//app/review/proto:file_java_library",
        "//app/review/storage:file_storage",
        "//app/review/storage:storage_library",
        "//dev/logos/service/storage:storage_library",
        "//dev/logos/service/storage/pg",
        "//dev/logos/user",
        "@maven//:com_google_inject_guice",
        "@maven//:org_antlr_antlr4_runtime",
    ],
)
