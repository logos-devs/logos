java_library(
    name = "cognito",
    srcs = [
        "CognitoServerInterceptor.java",
    ],
    visibility = ["//app:__subpackages__"],
    deps = [
        "//app/auth/cognito/module/annotation",
        "//app/auth/cognito/module/data",
        "//app/auth/cognito/user",
        "@logos//dev/logos/auth/user",
        "@logos//dev/logos/stack/aws/module/annotation",
        "@maven_logos//:com_google_code_gson_gson",
        "@maven_logos//:com_google_guava_guava",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:io_grpc_grpc_api",
        "@maven_logos//:io_grpc_grpc_context",
        "@maven_logos//:io_jsonwebtoken_jjwt_api",
        "@maven_logos//:io_jsonwebtoken_jjwt_impl",
        "@maven_logos//:io_jsonwebtoken_jjwt_jackson",
    ],
)

java_test(
    name = "test",
    srcs = ["CognitoServerInterceptorTest.java"],
    test_class = "app.auth.cognito.interceptor.cognito.CognitoServerInterceptorTest",
    deps = [
        ":cognito",
        "//app/auth/cognito/module/data",
        "@maven_logos//:io_jsonwebtoken_jjwt_api",
        "@maven_logos//:junit_junit",
    ],
)
