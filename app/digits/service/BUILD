load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")

java_grpc_library(
    name = "voice_java_grpc",
    protos = ["//app/digits/proto"],
)

java_library(
    name = "service",
    srcs = [
        "PhoneNumberStorageService.java",
        "VoiceService.java",
    ],
    visibility = [
        "//app/digits/module:__pkg__",
        "//dev/logos/service:__subpackages__",
    ],
    deps = [
        ":voice_java_grpc",
        "//app/digits/proto:java_library",
        "//app/digits/storage",
        "//dev/logos/client/twilio",
        "//dev/logos/service",
        "//dev/logos/service/storage/pg",
        "//dev/logos/user",
        "@maven//:com_google_inject_guice",
        "@maven//:com_twilio_sdk_twilio",
    ],
)
