java_library(
    name = "module",
    srcs = glob(["DatabaseModule.java"]),
    plugins = [
        "//dev/logos/app/register:module",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//dev/logos/app/register:module_library",
        "@maven_logos//:com_google_inject_guice",
        "@maven_logos//:com_zaxxer_HikariCP",
        "@maven_logos//:dnsjava_dnsjava",
        "@maven_logos//:org_jdbi_jdbi3_core",
        "@maven_logos//:org_jdbi_jdbi3_postgres",
        "@maven_logos//:org_postgresql_postgresql",
        "@maven_logos//:software_amazon_awssdk_auth",
        "@maven_logos//:software_amazon_awssdk_rds",
        "@maven_logos//:software_amazon_awssdk_regions",
        "@maven_logos//:software_amazon_awssdk_sso",
        "@maven_logos//:software_amazon_awssdk_ssooidc",
        "@maven_logos//:software_amazon_awssdk_sts",
        "@maven_logos//:software_amazon_awssdk_utils",
    ],
)
