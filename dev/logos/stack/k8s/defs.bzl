def k8s_manifest(name, deps, visibility = None):
    native.java_binary(
        name = name + "_synthesizer",
        main_class = "dev.logos.stack.k8s.synthesizer.Synthesizer",
        plugins = ["@logos//dev/logos/app/register:module"],
        visibility = visibility,
        runtime_deps = deps + ["@logos//dev/logos/stack/k8s/synthesizer"],
    )

    native.genrule(
        name = name,
        outs = ["stack.k8s.yaml"],
        cmd = "$(location :" + name + """_synthesizer) && \
                 (first=true; for f in dist/*.yaml; do \
                    if [ "$$first" = false ]; then \
                        echo "---" >> $(@D)/stack.k8s.yaml; \
                    fi; \
                    cat "$$f" >> $(@D)/stack.k8s.yaml; \
                    first=false; \
                 done) && rm -rf dist""",
        toolchains = ["@nodejs_toolchains//:resolved_toolchain"],
        tools = [
            "@nodejs_toolchains//:resolved_toolchain",
            ":" + name + "_synthesizer",
        ],
        tags = ["no-sandbox", "no-remote"],
        visibility = visibility,
    )
