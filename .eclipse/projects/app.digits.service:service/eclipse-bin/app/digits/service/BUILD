load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")

java_grpc_library(
    name = "voice_java_grpc",
    protos = ["//app/digits/proto:voice_proto"],
)

java_library(
    name = "service",
    srcs = [
        "PhoneNumberStorageService.java",
        "VoiceService.java",
    ],
    visibility = [
        "//app/digits/module:__pkg__",
        "//dev/logos/stack:__subpackages__",
    ],
    deps = [
        ":voice_java_grpc",
        "//app/digits/proto:voice_java_library",
        "//app/digits/storage:phone_number_storage",
        "//app/digits/storage:storage_proto",
        "//dev/logos/client/twilio",
        "//dev/logos/stack/service/storage:storage_library",
        "//dev/logos/stack/service/storage/pg",
        "//dev/logos/stack/service/storage/pg/meta:schema_export_library",
        "@maven//:com_google_inject_guice",
        "@maven//:com_twilio_sdk_twilio",
        "@maven//:javax_inject_javax_inject",
    ],
)
