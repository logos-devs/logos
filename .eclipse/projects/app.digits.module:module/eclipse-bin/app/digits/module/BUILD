java_library(
    name = "module",
    srcs = ["DigitsModule.java"],
    visibility = ["//dev/logos/stack:__subpackages__"],
    deps = [
        "//app/digits/proto:voice_java_library",
        "//app/digits/service",
        "//app/digits/storage:phone_number_storage",
        "//dev/logos/client/twilio",
        "//dev/logos/stack/service/storage/pg",
        "@io_grpc_grpc_java//api",
        "@maven//:com_google_inject_guice",
        "@maven//:com_twilio_sdk_twilio",
    ],
)
