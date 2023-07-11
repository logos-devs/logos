load("@rules_antlr//antlr:antlr4.bzl", "antlr")

antlr(
    name = "java9_parser",
    srcs = ["Java9.g4"],
    package = "dev.logos.grammar",
)

java_library(
    name = "java9_parser_java",
    srcs = [":java9_parser"],
    visibility = ["//visibility:public"],
    deps = ["@antlr4_runtime//jar"],
)

antlr(
    name = "aipname_parser",
    srcs = ["AIPName.g4"],
    package = "dev.logos.grammar",
)

java_library(
    name = "aipname_parser_java",
    srcs = [":aipname_parser"],
    visibility = ["//visibility:public"],
    deps = ["@antlr4_runtime//jar"],
)
