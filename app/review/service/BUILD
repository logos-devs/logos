java_library(
    name = "review_service",
    srcs = [
        "FileService.java",
        "ProjectService.java",
    ],
    visibility = ["//dev/logos/service:__subpackages__"],
    deps = [
        ":annotation_java_grpc",
        ":annotation_java_library",
        ":file_java_grpc",
        ":file_java_library",
        ":storage_library",
        "//app/review/grammar:aipname_parser_java",
        "//app/review/grammar:java9_parser_java",
        "//dev/logos/service/storage:storage_library",
        "//dev/logos/service/storage/pg",
        "//dev/logos/user",
        "@maven//:com_google_inject_guice",
        "@maven//:org_antlr_antlr4_runtime",
    ],
)
