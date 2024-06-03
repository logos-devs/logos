java_library(
    name = "module",
    srcs = ["DigitsModule.java"],
    plugins = [
        "//dev/logos/app/register:module",
    ],
    visibility = ["//dev/logos/service:__subpackages__"],
    deps = [
        "//app/digits/proto:voice_java_library",
        "//app/digits/service",
        "//app/digits/storage:phone_number_storage",
        "//app/digits/storage:storage_library",
        "//dev/logos/app/register:module_library",
        "//dev/logos/client/twilio",
        "//dev/logos/service/storage:storage_library",
        "//dev/logos/service/storage/pg",
        "@io_grpc_grpc_java//api",
        "@maven//:com_google_inject_guice",
        "@maven//:com_twilio_sdk_twilio",
    ],
)
