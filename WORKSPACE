workspace(name = "logos")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file")

# rules_python
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "rules_python",
    sha256 = "9acc0944c94adb23fba1c9988b48768b1bacc6583b52a2586895c5b7491e2e31",
    strip_prefix = "rules_python-0.27.0",
    url = "https://github.com/bazelbuild/rules_python/releases/download/0.27.0/rules_python-0.27.0.tar.gz",
)

load("@rules_python//python:repositories.bzl", "py_repositories", "python_register_toolchains")

py_repositories()

load("@rules_python//python:pip.bzl", "pip_parse")

pip_parse(
    name = "pip_deps",
    requirements_lock = "//vendor/python:requirements_lock.txt",
)

load("@pip_deps//:requirements.bzl", "install_deps")

install_deps()

# minikube
http_file(
    name = "minikube_linux",
    executable = True,
    sha256 = "e53d9e8c31f4c5f683182f5323d3527aa0725f713945c6d081cf71aa548ab388",
    url = "https://github.com/kubernetes/minikube/releases/download/v1.30.1/minikube-linux-amd64",
)

http_file(
    name = "minikube_osx",
    executable = True,
    sha256 = "b5938a8772c5565b5d0b795938c367c5190bf65bb51fc55fb2417cb4e1d04ef1",
    url = "https://github.com/kubernetes/minikube/releases/download/v1.30.1/minikube-darwin-amd64",
)

# eksctl
http_archive(
    name = "eksctl_linux",
    build_file_content = """
filegroup(
    name = "eksctl_linux",
    srcs = ["eksctl"],
    visibility = ["//visibility:public"]
)
    """,
    sha256 = "b16ba179d476997b236c40aa3c1c94306404b2aa189c64c1fd4631c47c64032c",
    url = "https://github.com/weaveworks/eksctl/releases/download/v0.132.0/eksctl_Linux_amd64.tar.gz",
)

#kubectl
http_file(
    name = "kubectl_linux",
    executable = True,
    sha256 = "7fe3a762d926fb068bae32c399880e946e8caf3d903078bea9b169dcd5c17f6d",
    url = "https://dl.k8s.io/release/v1.27.1/bin/linux/amd64/kubectl",
)

http_file(
    name = "kubectl_osx",
    executable = True,
    sha256 = "136f73ede0d52c7985d299432236f891515c050d58d71b4a7d39c45085020ad8",
    url = "https://dl.k8s.io/release/v1.27.1/bin/darwin/amd64/kubectl",
)

# rules_go
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "io_bazel_rules_go",
    sha256 = "51dc53293afe317d2696d4d6433a4c33feedb7748a9e352072e2ec3c0dafd2c6",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/rules_go/releases/download/v0.40.1/rules_go-v0.40.1.zip",
        "https://github.com/bazelbuild/rules_go/releases/download/v0.40.1/rules_go-v0.40.1.zip",
    ],
)

http_archive(
    name = "bazel_gazelle",
    sha256 = "727f3e4edd96ea20c29e8c2ca9e8d2af724d8c7778e7923a854b2c80952bc405",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/bazel-gazelle/releases/download/v0.30.0/bazel-gazelle-v0.30.0.tar.gz",
        "https://github.com/bazelbuild/bazel-gazelle/releases/download/v0.30.0/bazel-gazelle-v0.30.0.tar.gz",
    ],
)

load("@io_bazel_rules_go//go:deps.bzl", "go_register_toolchains", "go_rules_dependencies")
load("@bazel_gazelle//:deps.bzl", "gazelle_dependencies")

go_rules_dependencies()

go_register_toolchains(version = "1.20.7")

gazelle_dependencies()

# rules_proto
http_archive(
    name = "com_google_protobuf",
    sha256 = "0aa7df8289c957a4c54cbe694fbabe99b180e64ca0f8fdb5e2f76dcf56ff2422",
    strip_prefix = "protobuf-21.9",
    urls = ["https://github.com/protocolbuffers/protobuf/archive/refs/tags/v21.9.tar.gz"],
)

http_archive(
    name = "grpc_web",
    sha256 = "376937b22095bdbea00f8bcd9442c1824419a99cbc37caf0967e4a0fa8b16658",
    strip_prefix = "grpc-web-1.4.2",
    urls = ["https://github.com/grpc/grpc-web/archive/refs/tags/1.4.2.tar.gz"],
)

http_archive(
    name = "rules_proto",
    sha256 = "80d3a4ec17354cccc898bfe32118edd934f851b03029d63ef3fc7c8663a7415c",
    strip_prefix = "rules_proto-5.3.0-21.5",
    urls = [
        "https://github.com/bazelbuild/rules_proto/archive/refs/tags/5.3.0-21.5.tar.gz",
    ],
)

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

rules_proto_dependencies()

rules_proto_toolchains()

http_archive(
    name = "rules_proto_grpc",
    sha256 = "f87d885ebfd6a1bdf02b4c4ba5bf6fb333f90d54561e4d520a8413c8d1fb7beb",
    strip_prefix = "rules_proto_grpc-4.5.0",
    urls = ["https://github.com/rules-proto-grpc/rules_proto_grpc/archive/4.5.0.tar.gz"],
)

load("@rules_proto_grpc//:repositories.bzl", "rules_proto_grpc_repos", "rules_proto_grpc_toolchains")

rules_proto_grpc_toolchains()

rules_proto_grpc_repos()

load("@rules_proto_grpc//java:repositories.bzl", rules_proto_grpc_java_repos = "java_repos")

rules_proto_grpc_java_repos()

load("@rules_proto_grpc//js:repositories.bzl", rules_proto_grpc_js_repos = "js_repos")

rules_proto_grpc_js_repos()

# Java Maven deps
load("@io_grpc_grpc_java//:repositories.bzl", "IO_GRPC_GRPC_JAVA_ARTIFACTS", "IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS", "grpc_java_repositories")
load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "com.auth0:java-jwt:4.4.0",
        "com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:2.15.2",
        "com.google.inject:guice:7.0.0",
        "com.mysema.commons:mysema-commons-lang:0.2.4",
        "com.querydsl:codegen-utils:5.0.0",
        "com.querydsl:querydsl-codegen:5.0.0",
        "com.querydsl:querydsl-core:5.0.0",
        "com.querydsl:querydsl-sql-codegen:5.0.0",
        "com.querydsl:querydsl-sql:5.0.0",
        "com.twilio.sdk:twilio:9.8.0",
        "com.yubico:webauthn-server-core:2.5.0",
        "com.zaxxer:HikariCP:5.0.1",
        "dnsjava:dnsjava:3.5.2",
        "javax.annotation:javax.annotation-api:1.3.2",
        "javax.json:javax.json-api:1.1.4",
        "org.antlr:antlr4-runtime:4.13.0",
        "org.reflections:reflections:0.10.2",
        "org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r",
        "org.jdbi:jdbi3-core:3.39.1",
        "com.squareup:javapoet:1.13.0",
        "org.jdbi:jdbi3-postgres:3.35.0",
        "org.junit.jupiter:junit-jupiter-api:5.9.3",
        "org.postgresql:postgresql:42.6.0",
        "org.reactivestreams:reactive-streams:1.0.4",
        "org.slf4j:slf4j-api:2.0.7",
        "org.slf4j:slf4j-simple:2.0.7",
        "software.amazon.awssdk:auth:2.20.102",
        "software.amazon.awssdk:ec2:2.20.102",
        "software.amazon.awssdk:rds:2.20.102",
        "software.amazon.awssdk:s3:2.20.102",
        "software.amazon.awssdk:sso:2.20.102",
        "software.amazon.awssdk:ssooidc:2.20.102",
        "software.amazon.awssdk:secretsmanager:2.20.102",
        "software.amazon.awssdk:sts:2.20.102",
        "software.amazon.awssdk:regions:2.20.102",
        "software.amazon.awssdk:utils:2.20.102",
    ] + IO_GRPC_GRPC_JAVA_ARTIFACTS,
    fail_if_repin_required = True,
    fetch_sources = True,
    generate_compat_repositories = True,
    # bazel run @unpinned_maven//:pin
    maven_install_json = "//vendor/java:maven_install.json",
    override_targets = IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS,
    repositories = [
        "https://repo.maven.apache.org/maven2/",
    ],
    version_conflict_policy = "pinned",
)

load("@maven//:defs.bzl", "pinned_maven_install")

pinned_maven_install()

load("@maven//:compat.bzl", "compat_repositories")

compat_repositories()

grpc_java_repositories()

# google_bazel_common (javadoc for server)
http_archive(
    name = "google_bazel_common",
    sha256 = "06b26b6f0239182f39bf879c8668daa8c15c6b58c35c5da694d0df7dfd596fd2",
    strip_prefix = "bazel-common-a482a3abeaa12b48e78c012ab44407da9c97a400",
    urls = ["https://github.com/google/bazel-common/archive/a482a3abeaa12b48e78c012ab44407da9c97a400.zip"],
)

load("@google_bazel_common//:workspace_defs.bzl", "google_common_workspace_rules")

google_common_workspace_rules()

# rules_antlr
http_archive(
    name = "rules_antlr",
    sha256 = "26e6a83c665cf6c1093b628b3a749071322f0f70305d12ede30909695ed85591",
    strip_prefix = "rules_antlr-0.5.0",
    urls = ["https://github.com/marcohu/rules_antlr/archive/0.5.0.tar.gz"],
)

load("@rules_antlr//antlr:repositories.bzl", "rules_antlr_dependencies")

rules_antlr_dependencies("4.8")

# rules_js
http_archive(
    name = "aspect_rules_js",
    sha256 = "d9ceb89e97bb5ad53b278148e01a77a3e9100db272ce4ebdcd59889d26b9076e",
    strip_prefix = "rules_js-1.34.0",
    url = "https://github.com/aspect-build/rules_js/releases/download/v1.34.0/rules_js-v1.34.0.tar.gz",
)

load("@aspect_rules_js//js:repositories.bzl", "rules_js_dependencies")

rules_js_dependencies()

load("@rules_nodejs//nodejs:repositories.bzl", "DEFAULT_NODE_VERSION", "nodejs_register_toolchains")

nodejs_register_toolchains(
    name = "nodejs",
    node_version = "18.13.0",
)

load("@aspect_rules_js//npm:repositories.bzl", "npm_translate_lock")

npm_translate_lock(
    name = "npm",
    pnpm_lock = "//:pnpm-lock.yaml",
    verify_node_modules_ignored = "//:.bazelignore",
)

load("@npm//:repositories.bzl", "npm_repositories")

npm_repositories()

# rules_ts
http_archive(
    name = "aspect_rules_ts",
    sha256 = "4c3f34fff9f96ffc9c26635d8235a32a23a6797324486c7d23c1dfa477e8b451",
    strip_prefix = "rules_ts-1.4.5",
    url = "https://github.com/aspect-build/rules_ts/releases/download/v1.4.5/rules_ts-v1.4.5.tar.gz",
)

load("@aspect_rules_ts//ts:repositories.bzl", "rules_ts_dependencies")

rules_ts_dependencies(
    ts_integrity = "sha512-mI4WrpHsbCIcwT9cF4FZvr80QUeKvsUsUvKDoR+X/7XHQH98xYD8YHZg7ANtz2GtZt/CBq2QJ0thkGJMHfqc1w==",
    ts_version_from = "//:package.json",
)

# protobuf_javascript
http_archive(
    name = "protobuf_javascript",
    sha256 = "35bca1729532b0a77280bf28ab5937438e3dcccd6b31a282d9ae84c896b6f6e3",
    strip_prefix = "protobuf-javascript-3.21.2",
    url = "https://github.com/protocolbuffers/protobuf-javascript/archive/refs/tags/v3.21.2.tar.gz",
)

# buildbuddy
http_archive(
    name = "io_buildbuddy_buildbuddy_toolchain",
    sha256 = "e899f235b36cb901b678bd6f55c1229df23fcbc7921ac7a3585d29bff2bf9cfd",
    strip_prefix = "buildbuddy-toolchain-fd351ca8f152d66fc97f9d98009e0ae000854e8f",
    urls = ["https://github.com/buildbuddy-io/buildbuddy-toolchain/archive/fd351ca8f152d66fc97f9d98009e0ae000854e8f.tar.gz"],
)

load("@io_buildbuddy_buildbuddy_toolchain//:deps.bzl", "buildbuddy_deps")

buildbuddy_deps()

load("@io_buildbuddy_buildbuddy_toolchain//:rules.bzl", "UBUNTU20_04_IMAGE", "buildbuddy")

buildbuddy(
    name = "buildbuddy_toolchain",
    container_image = UBUNTU20_04_IMAGE,
    llvm = True,
)

# jq
load("@aspect_bazel_lib//lib:repositories.bzl", "register_jq_toolchains")

register_jq_toolchains()

# yq
load("@aspect_bazel_lib//lib:repositories.bzl", "register_yq_toolchains")

register_yq_toolchains()
