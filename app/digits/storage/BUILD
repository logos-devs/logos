load("//dev/logos/stack/service/storage/pg/meta:schema_export.bzl", "schema_export", "storage_java_proto_library")

java_library(
    name = "phone_number_storage",
    srcs = [
        "PhoneNumberStorage.java",
    ],
    visibility = ["//app/digits:__subpackages__"],
    deps = [
        ":storage_proto",
        "//app/digits/proto:voice_java_library",
        "//dev/logos/stack/service/storage:storage_library",
        "//dev/logos/stack/service/storage/pg",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//protobuf",
        "@io_grpc_grpc_java//stub",
        "@maven//:com_google_inject_guice",
        "@maven//:javax_inject_javax_inject",
    ],
)

schema_export(
    name = "schema_export",
    tables = {
        "digits": ["phone_number"],
    },
    visibility = ["//app/digits:__subpackages__"],
)

storage_java_proto_library(
    name = "storage_proto",
    srcs = [":schema_export"],
    visibility = ["//app/digits:__subpackages__"],
)
