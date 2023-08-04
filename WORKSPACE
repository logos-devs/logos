workspace(name = "logos")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file")

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
http_archive(
    name = "io_bazel_rules_go",
    sha256 = "dd926a88a564a9246713a9c00b35315f54cbd46b31a26d5d8fb264c07045f05d",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/rules_go/releases/download/v0.38.1/rules_go-v0.38.1.zip",
        "https://github.com/bazelbuild/rules_go/releases/download/v0.38.1/rules_go-v0.38.1.zip",
    ],
)

http_archive(
    name = "bazel_gazelle",
    sha256 = "ecba0f04f96b4960a5b250c8e8eeec42281035970aa8852dda73098274d14a1d",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/bazel-gazelle/releases/download/v0.29.0/bazel-gazelle-v0.29.0.tar.gz",
        "https://github.com/bazelbuild/bazel-gazelle/releases/download/v0.29.0/bazel-gazelle-v0.29.0.tar.gz",
    ],
)

load("@io_bazel_rules_go//go:deps.bzl", "go_register_toolchains", "go_rules_dependencies")
load("@bazel_gazelle//:deps.bzl", "gazelle_dependencies", "go_repository")

go_rules_dependencies()

go_register_toolchains(version = "1.19.5")

gazelle_dependencies()

# buildifier
http_archive(
    name = "com_github_bazelbuild_buildtools",
    sha256 = "ae34c344514e08c23e90da0e2d6cb700fcd28e80c02e23e4d5715dddcb42f7b3",
    strip_prefix = "buildtools-4.2.2",
    urls = [
        "https://github.com/bazelbuild/buildtools/archive/refs/tags/4.2.2.tar.gz",
    ],
)

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
    sha256 = "bbe4db93499f5c9414926e46f9e35016999a4e9f6e3522482d3760dc61011070",
    strip_prefix = "rules_proto_grpc-4.2.0",
    urls = ["https://github.com/rules-proto-grpc/rules_proto_grpc/archive/4.2.0.tar.gz"],
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
        "io.grpc:grpc-netty-shaded:1.56.1",
        "io.grpc:grpc-protobuf:1.56.1",
        "io.grpc:grpc-services:1.56.1",
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
    maven_install_json = "//:maven_install.json",
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
    sha256 = "bcb5de5a5b951434828ead94479d3e1ff6501c2c8fc490db6cf3fbf7c188684b",
    strip_prefix = "bazel-common-aaa4d801588f7744c6f4428e4f133f26b8518f42",
    urls = ["https://github.com/google/bazel-common/archive/aaa4d801588f7744c6f4428e4f133f26b8518f42.zip"],
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
    sha256 = "e3e6c3d42491e2938f4239a3d04259a58adc83e21e352346ad4ef62f87e76125",
    strip_prefix = "rules_js-1.30.0",
    url = "https://github.com/aspect-build/rules_js/releases/download/v1.30.0/rules_js-v1.30.0.tar.gz",
)

load("@aspect_rules_js//js:repositories.bzl", "rules_js_dependencies")

rules_js_dependencies()

load("@rules_nodejs//nodejs:repositories.bzl", "DEFAULT_NODE_VERSION", "nodejs_register_toolchains")

nodejs_register_toolchains(
    name = "nodejs",
    node_version = DEFAULT_NODE_VERSION,
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
    ts_version_from = "//:package.json",
)

# protobuf_javascript
http_archive(
    name = "protobuf_javascript",
    sha256 = "35bca1729532b0a77280bf28ab5937438e3dcccd6b31a282d9ae84c896b6f6e3",
    strip_prefix = "protobuf-javascript-3.21.2",
    url = "https://github.com/protocolbuffers/protobuf-javascript/archive/refs/tags/v3.21.2.tar.gz",
)

# rules_docker
http_archive(
    name = "io_bazel_rules_docker",
    sha256 = "b1e80761a8a8243d03ebca8845e9cc1ba6c82ce7c5179ce2b295cd36f7e394bf",
    urls = ["https://github.com/bazelbuild/rules_docker/releases/download/v0.25.0/rules_docker-v0.25.0.tar.gz"],
)

load("@io_bazel_rules_docker//repositories:repositories.bzl", container_repositories = "repositories")

container_repositories()

load("@io_bazel_rules_docker//repositories:deps.bzl", container_deps = "deps")

container_deps()

load("@io_bazel_rules_docker//container:container.bzl", "container_pull")

container_pull(
    name = "envoy_distroless_container",
    digest = "sha256:5d5619b0d3d4311bfef48783c04df1e5aed71615710b876b951bd70c1e1e18ee",
    registry = "docker.io",
    repository = "envoyproxy/envoy-distroless",
    tag = "v1.21-latest",
)

container_pull(
    name = "debian_distroless_container",
    digest = "sha256:c7151ad9a6787379ccf205e14b9ef18e0757e56d789c155269fc7b5f83d2a93e",
    registry = "gcr.io",
    repository = "distroless/java17-debian11",
    tag = "latest",
)

container_pull(
    name = "console_container",
    digest = "sha256:1304f174557314a7ed9eddb4eab12fed12cb0cd9809e4c28f29af86979a3c870",
    registry = "docker.io",
    repository = "alpine",
    tag = "latest",
)

#
container_pull(
    name = "nginx_container",
    digest = "sha256:f0219f99a56ec88f02f1d6055e5b4565cb80dba10b2301aa18f47913456accac",
    registry = "docker.io",
    repository = "nginx",
    tag = "stable-alpine",
)

container_pull(
    name = "stolon_container",
    digest = "sha256:5d8a8292d9a008ba29cbe0285bd5d4d041a75945e1ffedb14b1bfbee5b2414fa",
    registry = "docker.io",
    repository = "sorintlab/stolon",
    tag = "master-pg14",
)

load("@io_bazel_rules_docker//java:image.bzl", _java_image_repos = "repositories")

_java_image_repos()

# rules_gitops

http_archive(
    name = "com_adobe_rules_gitops",
    sha256 = "109c0a80d7bc45c15cad141d8f8245c93dcd88cb76229ab92600c1fe9c376caa",
    strip_prefix = "rules_gitops-0.18.0",
    urls = ["https://github.com/adobe/rules_gitops/archive/refs/tags/v0.18.0.tar.gz"],
)

load("@com_adobe_rules_gitops//gitops:deps.bzl", "rules_gitops_dependencies")

rules_gitops_dependencies()

load("@com_adobe_rules_gitops//gitops:repositories.bzl", "rules_gitops_repositories")

rules_gitops_repositories()
