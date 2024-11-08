load("//bzl:k8s.bzl", "kubectl")
load("@rules_oci//oci:defs.bzl", "oci_image")
load("@rules_pkg//pkg:pkg.bzl", "pkg_tar")
load("@aspect_bazel_lib//lib:tar.bzl", "tar")
load("//bzl:k8s.bzl", "kubectl")
load("//bzl:push_image.bzl", "push_image")

java_binary(
    name = "controller_bin",
    srcs = [
        "AppController.java",
        "Route53ZoneCreator.java",
    ],
    main_class = "dev.logos.app.controller.AppController",
    deps = [
        "@maven_logos_app_controller//:com_google_code_gson_gson",
        "@maven_logos_app_controller//:com_google_inject_guice",
        "@maven_logos_app_controller//:io_grpc_grpc_api",
        "@maven_logos_app_controller//:io_kubernetes_client_java",
        "@maven_logos_app_controller//:io_kubernetes_client_java_api",
        "@maven_logos_app_controller//:org_slf4j_slf4j_api",
        "@maven_logos_app_controller//:org_slf4j_slf4j_simple",
        "@maven_logos_app_controller//:software_amazon_awssdk_auth",
        "@maven_logos_app_controller//:software_amazon_awssdk_regions",
        "@maven_logos_app_controller//:software_amazon_awssdk_route53",
        "@maven_logos_app_controller//:software_amazon_awssdk_sso",
        "@maven_logos_app_controller//:software_amazon_awssdk_ssooidc",
    ],
)

pkg_tar(
    name = "image_jar",
    srcs = [":controller_bin_deploy.jar"],
)

oci_image(
    name = "image",
    base = "@distroless_java_container",
    entrypoint = [
        "java",
        "-jar",
        "/controller_bin_deploy.jar",
    ],
    tars = [":image_jar"],
)

push_image(
    name = "image_push",
    image = ":image",
    remote_tags = ["latest"],
    repository = "logos-ecr-app-controller",
)

kubectl(
    name = "controller",
    image_pushes = [":image_push"],
    images = {":image.digest": "logos-ecr-app-controller"},
    manifests = [
        "manifests/serviceaccount.yaml",
        "manifests/clusterrole.yaml",
        "manifests/clusterrolebinding.yaml",
        "manifests/customresourcedefinition.yaml",
        "manifests/configmap.yaml",
        "manifests/deployment.yaml",
    ],
    visibility = ["//dev/logos:__subpackages__"],
)
