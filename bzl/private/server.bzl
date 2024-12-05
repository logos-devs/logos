def server(name, srcs = None, deps = None, visibility = None):
    native.java_binary(
        name = name,
        srcs = srcs,
        main_class = "dev.logos.service.backend.server.ServerExecutor",
        plugins = ["@logos//dev/logos/app/register:module"],
        visibility = visibility,
        runtime_deps = [
            "@logos//dev/logos/service/backend/server",
        ] + (deps or []),
    )
