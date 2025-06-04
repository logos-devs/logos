java_library(
    name = "cookie",
    srcs = [
        "CookieServerInterceptor.java",
    ],
    visibility = ["//app:__subpackages__"],
    deps = [
        "@maven_logos//:com_google_code_gson_gson",
        "@maven_logos//:io_grpc_grpc_api",
        "@maven_logos//:io_grpc_grpc_context",
    ],
)

java_test(
    name = "test",
    srcs = ["CookieServerInterceptorTest.java"],
    test_class = "app.auth.cognito.interceptor.cookie.CookieServerInterceptorTest",
    deps = [
        ":cookie",
        "@grpc_java_bzlmod//api",
        "@maven_logos//:org_mockito_mockito_core",
        "@maven_logos//:junit_junit",
    ],
)
