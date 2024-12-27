package dev.logos.service.storage.pg.exporter.module;

import com.google.inject.AbstractModule;
import dev.logos.service.storage.pg.exporter.module.annotation.BuildDir;
import dev.logos.service.storage.pg.exporter.module.annotation.BuildPackage;

public class ExportModule extends AbstractModule {
    private final String buildDir;
    private final String buildPackage;

    public ExportModule(String buildDir, String buildPackage) {
        this.buildDir = buildDir;
        this.buildPackage = buildPackage;
    }

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(BuildDir.class).toInstance(buildDir);
        bind(String.class).annotatedWith(BuildPackage.class).toInstance(buildPackage);
    }
}
