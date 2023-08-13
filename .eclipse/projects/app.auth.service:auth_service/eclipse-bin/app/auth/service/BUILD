load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")

java_grpc_library(
    name = "auth_java_grpc",
    protos = ["//app/auth/proto:auth_proto"],
)

java_library(
    name = "auth_service",
    srcs = glob(["**/*.java"]),
    visibility = [
        "//app/auth/module:__subpackages__",
        "//dev/logos/stack:__subpackages__",
    ],
    deps = [
        ":auth_java_grpc",
        "//app/auth/proto:auth_java_library",
        "//app/auth/storage:auth_storage",
        "//dev/logos/stack/service/storage:storage_library",
        "@maven//:com_auth0_java_jwt",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_google_inject_guice",
        "@maven//:com_yubico_webauthn_server_core",
        "@maven//:javax_inject_javax_inject",
        "@maven//:javax_json_javax_json_api",
    ],
)
