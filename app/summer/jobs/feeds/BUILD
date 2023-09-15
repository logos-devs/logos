load("@rules_python//python:defs.bzl", "py_binary")
load("@pip//:requirements.bzl", "requirement")

py_binary(
    name = "rss",
    srcs = ["rss.py"],
    deps = [
        requirement("boto3"),
        requirement("feedparser"),
        requirement("openai"),
        requirement("psycopg"),
        requirement("requests"),
        requirement("trafilatura"),
    ],
)
