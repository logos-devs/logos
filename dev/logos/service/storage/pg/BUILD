java_library(
    name = "pg",
    srcs = glob([
        "*.java",
    ]),
    visibility = [
        "//app:__subpackages__",
        "//dev/logos/module:__subpackages__",
        "//dev/logos/service:__subpackages__",
    ],
    deps = [
        "@com_google_protobuf//:protobuf_java",
        "@maven//:javax_inject_javax_inject",
        "@maven//:org_jdbi_jdbi3_core",
        "@maven//:org_jdbi_jdbi3_postgres",
    ],
)
