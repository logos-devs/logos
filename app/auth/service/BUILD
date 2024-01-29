load("@rules_proto_grpc//java:defs.bzl", "java_grpc_library")

java_grpc_library(
    name = "auth_java_grpc",
    protos = ["//app/auth/proto"],
)

java_library(
    name = "service",
    srcs = glob(["**/*.java"]),
    visibility = [
        "//app/auth/module:__subpackages__",
        "//dev/logos/service:__subpackages__",
    ],
    deps = [
        ":auth_java_grpc",
        "//app/auth/module/data",
        "//app/auth/proto:java",
        "//app/auth/storage:auth_storage",
        "//dev/logos/service/backend:cookie_server_interceptor_library",
        "//dev/logos/service/storage:storage_library",
        "@io_grpc_grpc_java//context",
        "@maven//:com_auth0_java_jwt",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_inject_guice",
        "@maven//:com_yubico_webauthn_server_core",
        "@maven//:javax_inject_javax_inject",
        "@maven//:javax_json_javax_json_api",
        "@maven//:org_apache_httpcomponents_client5_httpclient5",
        "@maven//:org_apache_httpcomponents_httpclient",
        "@maven//:org_apache_httpcomponents_httpcore",
        "@maven//:software_amazon_awssdk_cognitoidentityprovider",
        "@maven//:software_amazon_awssdk_http_client_spi",
        "@maven//:software_amazon_awssdk_secretsmanager",
    ],
)
