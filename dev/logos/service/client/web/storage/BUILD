load("//bzl:ts.bzl", "ts_library")

ts_library(
    name = "storage",
    srcs = [
        "client.ts",
        "entity/create.ts",
        "entity/edit.ts",
        "entity/list.ts",
        "event.ts",
        "index.ts",
    ],
    visibility = ["//app:__subpackages__"],
    deps = [
        "//:node_modules/@material/web",
        "//:node_modules/lit",
        "//:node_modules/lit-html",
    ],
)
