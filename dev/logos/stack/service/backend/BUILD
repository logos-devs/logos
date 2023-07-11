load("@io_bazel_rules_docker//container:container.bzl", "container_image")
load("@io_bazel_rules_docker//contrib:test.bzl", "container_test")
load("@io_bazel_rules_docker//java:image.bzl", "java_image")
load("@google_bazel_common//tools/javadoc:javadoc.bzl", "javadoc_library")
load("//:gitops.bzl", "k8s_deploy")

filegroup(
    name = "server_srcs",
    srcs = glob([
        "**/*.java",
    ]),
)

java_library(
    name = "server_library",
    srcs = [":server_srcs"],
    visibility = ["//visibility:public"],
    deps = [
        "//app/auth/module",
        "//app/auth/service:auth_service",
        "//app/auth/storage:auth_storage",
        "//app/digits/module",
        "//app/review:review_service",
        "//app/review/grammar:aipname_parser_java",
        "//app/review/grammar:java9_parser_java",
        "//dev/logos/stack/job",
        "//dev/logos/stack/module:database_module",
        "//dev/logos/stack/service/storage:storage_library",
        "@io_grpc_grpc_java//api",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_fasterxml_jackson_dataformat_jackson_dataformat_cbor",
        "@maven//:com_fasterxml_jackson_dataformat_jackson_dataformat_xml",
        "@maven//:com_fasterxml_jackson_datatype_jackson_datatype_jdk8",
        "@maven//:com_fasterxml_jackson_datatype_jackson_datatype_jsr310",
        "@maven//:com_fasterxml_woodstox_woodstox_core",
        "@maven//:com_github_peteroupc_numbers",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_errorprone_error_prone_annotations",
        "@maven//:com_google_inject_guice",
        "@maven//:com_google_j2objc_j2objc_annotations",
        "@maven//:com_googlecode_javaewah_JavaEWAH",
        "@maven//:com_sun_activation_jakarta_activation",
        "@maven//:com_twilio_sdk_twilio",
        "@maven//:com_upokecenter_cbor",
        "@maven//:com_yubico_webauthn_server_core",
        "@maven//:com_yubico_yubico_util",
        "@maven//:commons_codec_commons_codec",
        "@maven//:commons_io_commons_io",
        "@maven//:commons_logging_commons_logging",
        "@maven//:io_jsonwebtoken_jjwt_api",
        "@maven//:io_jsonwebtoken_jjwt_impl",
        "@maven//:io_jsonwebtoken_jjwt_jackson",
        "@maven//:io_netty_netty_buffer",
        "@maven//:io_netty_netty_codec",
        "@maven//:io_netty_netty_codec_http",
        "@maven//:io_netty_netty_codec_http2",
        "@maven//:io_netty_netty_common",
        "@maven//:io_netty_netty_handler",
        "@maven//:io_netty_netty_resolver",
        "@maven//:io_netty_netty_transport",
        "@maven//:io_netty_netty_transport_classes_epoll",
        "@maven//:io_netty_netty_transport_native_unix_common",
        "@maven//:io_perfmark_perfmark_api",
        "@maven//:io_r2dbc_r2dbc_spi",
        "@maven//:jakarta_xml_bind_jakarta_xml_bind_api",
        "@maven//:javax_activation_javax_activation_api",
        "@maven//:javax_inject_javax_inject",
        "@maven//:javax_json_javax_json_api",
        "@maven//:javax_xml_bind_jaxb_api",
        "@maven//:net_i2p_crypto_eddsa",
        "@maven//:org_antlr_antlr4_runtime",
        "@maven//:org_apache_commons_commons_dbcp2",
        "@maven//:org_apache_commons_commons_pool2",
        "@maven//:org_apache_httpcomponents_httpclient",
        "@maven//:org_apache_httpcomponents_httpcore",
        "@maven//:org_apache_tomcat_annotations_api",
        "@maven//:org_apiguardian_apiguardian_api",
        "@maven//:org_checkerframework_checker_qual",
        "@maven//:org_codehaus_mojo_animal_sniffer_annotations",
        "@maven//:org_codehaus_woodstox_stax2_api",
        "@maven//:org_eclipse_jgit_org_eclipse_jgit",
        "@maven//:org_glassfish_javax_json",
        "@maven//:org_junit_jupiter_junit_jupiter_api",
        "@maven//:org_junit_platform_junit_platform_commons",
        "@maven//:org_opentest4j_opentest4j",
        "@maven//:org_postgresql_postgresql",
        "@maven//:org_reactivestreams_reactive_streams",
        "@maven//:org_reflections_reflections",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_slf4j_slf4j_simple",
        "@maven//:software_amazon_awssdk_annotations",
        "@maven//:software_amazon_awssdk_apache_client",
        "@maven//:software_amazon_awssdk_arns",
        "@maven//:software_amazon_awssdk_auth",
        "@maven//:software_amazon_awssdk_aws_core",
        "@maven//:software_amazon_awssdk_aws_json_protocol",
        "@maven//:software_amazon_awssdk_aws_query_protocol",
        "@maven//:software_amazon_awssdk_aws_xml_protocol",
        "@maven//:software_amazon_awssdk_ec2",
        "@maven//:software_amazon_awssdk_http_client_spi",
        "@maven//:software_amazon_awssdk_json_utils",
        "@maven//:software_amazon_awssdk_metrics_spi",
        "@maven//:software_amazon_awssdk_netty_nio_client",
        "@maven//:software_amazon_awssdk_profiles",
        "@maven//:software_amazon_awssdk_protocol_core",
        "@maven//:software_amazon_awssdk_regions",
        "@maven//:software_amazon_awssdk_s3",
        "@maven//:software_amazon_awssdk_sdk_core",
        "@maven//:software_amazon_awssdk_secretsmanager",
        "@maven//:software_amazon_awssdk_utils",
        "@maven//:software_amazon_eventstream_eventstream",
    ],
)

javadoc_library(
    name = "docs",
    srcs = [
        ":server_srcs",
    ],
    deps = [
        ":server_library",
    ],
)

java_binary(
    name = "Server",
    main_class = "dev.logos.stack.service.backend.server.Server",
    visibility = ["//visibility:public"],
    runtime_deps = [":server_library"],
)

java_image(
    name = "server_image",
    main_class = "dev.logos.server.Server",
    visibility = ["//visibility:public"],
    runtime_deps = [":server_library"],
)

container_image(
    name = "image",
    base = "@debian_distroless_container//image",
    cmd = ["/Server_deploy.jar"],
    files = [":Server_deploy.jar"],
)

k8s_deploy(
    name = "backend",
    images = {
        "backend-image:latest": ":image",
    },
    manifests = [
        "manifests/deployment.yaml",
        "manifests/service.yaml",
    ],
    objects = [
        "//dev/logos/stack/service/storage",
    ],
    visibility = ["//dev/logos/stack:__subpackages__"],
)

# Routes traffic to a webpack-dev-server instance residing on the minikube host machine.
# Note : This service should *ONLY* be used in local development!
k8s_deploy(
    name = "service_hot",
    manifests = ["manifests/service_hot.yaml"],
    visibility = ["//dev/logos/stack:__subpackages__"],
)
