java_library(
    name = "module_library",
    srcs = [
        "RegisterModuleProcessor.java",
        "registerModule.java",
    ],
    resources = [
        "META-INF/services/javax.annotation.processing.Processor",
    ],
    visibility = [
        "//app:__subpackages__",
        "//dev/logos:__subpackages__",
    ],
    deps = [
        "@maven//:com_google_inject_guice",
    ],
)

java_plugin(
    name = "module",
    processor_class = "dev.logos.module.RegisterModuleProcessor",
    visibility = [
        "//app:__subpackages__",
        "//dev/logos:__subpackages__",
    ],
    deps = [":module_library"],
)
