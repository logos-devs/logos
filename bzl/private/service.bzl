def service(name, srcs, deps = None, visibility = None):
    native.java_library(
        name = name,
        srcs = srcs,
        visibility = visibility,
        deps = [
            "@io_grpc_grpc_java//stub",
            "@logos//dev/logos/service",
            "@logos//dev/logos/service/storage/pg",
            "@logos//dev/logos/service/storage/validator",
            "@logos//dev/logos/user",
            "@maven_logos//:com_google_inject_guice",
        ] + (deps or []),
    )
