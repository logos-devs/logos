def service(name, srcs, deps = None, visibility = None, **kwargs):
    native.java_library(
        name = name,
        srcs = srcs,
        visibility = visibility,
        deps = [
            "@logos//dev/logos/client/module",
            "@logos//dev/logos/service",
            "@logos//dev/logos/service/storage/pg",
            "@logos//dev/logos/service/storage/validator",
            "@logos//dev/logos/auth/user",
            "@maven_logos//:com_google_inject_guice",
            "@maven_logos//:io_grpc_grpc_stub",
        ] + (deps or []),
        **kwargs
    )
