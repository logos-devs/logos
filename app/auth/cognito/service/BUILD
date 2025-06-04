java_library(
    name = "service",
    srcs = ["CognitoService.java"],
    visibility = [
        "//app/auth/cognito/module:__subpackages__",
    ],
    deps = [
        "//app/auth/cognito/interceptor/cookie",
        "//app/auth/cognito/module/annotation",
        "//app/auth/cognito/module/data",
        "//app/auth/cognito/proto:java_grpc",
        "//app/auth/cognito/proto:java_proto",
        "@logos//dev/logos/auth/user",
        "@logos//dev/logos/service",
        "@maven_logos//:com_google_code_gson_gson",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:io_grpc_grpc_api",
        "@maven_logos//:io_grpc_grpc_context",
        "@maven_logos//:io_grpc_grpc_stub",
        "@maven_logos//:org_apache_httpcomponents_client5_httpclient5",
        "@maven_logos//:org_apache_httpcomponents_httpclient",
        "@maven_logos//:org_apache_httpcomponents_httpcore",
        "@maven_logos//:software_amazon_awssdk_cognitoidentityprovider",
        "@maven_logos//:software_amazon_awssdk_http_client_spi",
        "@maven_logos//:software_amazon_awssdk_secretsmanager",
    ],
)

java_test(
    name = "test",
    srcs = ["CognitoServiceTest.java"],
    test_class = "app.auth.cognito.service.CognitoServiceTest",
    deps = [
        ":service",
        "//app/auth/cognito/module/data",
        "//app/auth/cognito/proto:java_proto",
        "//dev/logos/auth/user",
        "@grpc_java_bzlmod//api",
        "@grpc_java_bzlmod//stub",
        "@maven_logos//:org_mockito_mockito_core",
        "@maven_logos//:junit_junit",
    ],
)
