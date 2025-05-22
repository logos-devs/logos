package dev.logos.service.storage.pg.exporter.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import dev.logos.service.storage.pg.exporter.codegen.column.ColumnGenerator;
import dev.logos.service.storage.pg.exporter.codegen.module.StorageModuleGenerator;
import dev.logos.service.storage.pg.exporter.codegen.proto.ProtoGenerator;
import dev.logos.service.storage.pg.exporter.codegen.qualifier.QualifierGenerator;
import dev.logos.service.storage.pg.exporter.codegen.schema.SchemaGenerator;
import dev.logos.service.storage.pg.exporter.codegen.service.StorageServiceBaseGenerator;
import dev.logos.service.storage.pg.exporter.codegen.storage.TableStorageGenerator;
import dev.logos.service.storage.pg.exporter.codegen.table.TableGenerator;
import dev.logos.service.storage.pg.exporter.codegen.type.*;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;
import dev.logos.service.storage.pg.exporter.module.annotation.BuildDir;
import dev.logos.service.storage.pg.exporter.module.annotation.BuildPackage;

import java.util.List;

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

        bind(SchemaGenerator.class).asEagerSingleton();
        bind(TableGenerator.class).asEagerSingleton();
        bind(ColumnGenerator.class).asEagerSingleton();
        bind(ProtoGenerator.class).asEagerSingleton();
        bind(StorageServiceBaseGenerator.class).asEagerSingleton();
        bind(StorageModuleGenerator.class).asEagerSingleton();
        bind(TableStorageGenerator.class).asEagerSingleton();
        bind(QualifierGenerator.class).asEagerSingleton();

        MapBinder<String, PgTypeMapper> pgTypeMapperBinder =
                MapBinder.newMapBinder(binder(), String.class, PgTypeMapper.class);

        List.of(
                    new IntegerMapper(),
                    new BigIntMapper(),
                    new FloatMapper(),
                    new DoubleMapper(),
                    new NumericMapper(),
                    new StringMapper(),
                    new TimestampMapper(),
                    new DateMapper(),
                    new BooleanMapper(),
                    new UuidMapper(),
                    new ByteaMapper()
            )
            .forEach((PgTypeMapper typeMapper) ->
                    typeMapper.getPgTypes()
                              .forEach((String pgType) -> pgTypeMapperBinder.addBinding(pgType).toInstance(typeMapper)));
    }
}