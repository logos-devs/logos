package dev.logos.service.storage.pg.exporter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import dev.logos.module.ModuleLoader;
import dev.logos.service.storage.pg.exporter.codegen.column.ColumnGenerator;
import dev.logos.service.storage.pg.exporter.codegen.module.StorageModuleGenerator;
import dev.logos.service.storage.pg.exporter.codegen.proto.ProtoGenerator;
import dev.logos.service.storage.pg.exporter.codegen.schema.SchemaGenerator;
import dev.logos.service.storage.pg.exporter.codegen.service.StorageServiceBaseGenerator;
import dev.logos.service.storage.pg.exporter.codegen.table.TableGenerator;
import dev.logos.service.storage.pg.exporter.descriptor.SchemaDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.TableDescriptor;
import dev.logos.service.storage.pg.exporter.module.ExportModule;
import dev.logos.service.storage.pg.exporter.module.annotation.BuildDir;
import dev.logos.service.storage.pg.exporter.module.annotation.BuildPackage;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;


public class Exporter {
    private final String buildDir;
    private final String buildPackage;
    private final DataSource dataSource;
    private final SchemaGenerator schemaGenerator;
    private final TableGenerator tableGenerator;
    private final ColumnGenerator columnGenerator;
    private final ProtoGenerator protoGenerator;
    private final StorageServiceBaseGenerator storageServiceBaseGenerator;
    private final StorageModuleGenerator storageModuleGenerator;
    private final Gson gson = new GsonBuilder().create();

    @Inject
    public Exporter(
            @BuildDir String buildDir,
            @BuildPackage String buildPackage,
            DataSource dataSource,
            SchemaGenerator schemaGenerator,
            TableGenerator tableGenerator,
            ColumnGenerator columnGenerator,
            ProtoGenerator protoGenerator,
            StorageServiceBaseGenerator storageServiceBaseGenerator,
            StorageModuleGenerator storageModuleGenerator
    ) {
        this.buildDir = buildDir;
        this.buildPackage = buildPackage;
        this.dataSource = dataSource;
        this.schemaGenerator = schemaGenerator;
        this.tableGenerator = tableGenerator;
        this.columnGenerator = columnGenerator;
        this.protoGenerator = protoGenerator;
        this.storageServiceBaseGenerator = storageServiceBaseGenerator;
        this.storageModuleGenerator = storageModuleGenerator;
    }

    private enum ExportType {
        JSON,
        PROTO,
        JAVA
    }

    public void exportJson(String tablesJson) throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            List<SchemaDescriptor> schemaDescriptors;
            schemaDescriptors = SchemaDescriptor.extract(
                    connection,
                    new Gson().fromJson(tablesJson, new TypeToken<Map<String, List<String>>>() {
                    }.getType()));

            try (OutputStream outputStream = Files.newOutputStream(
                    Files.createDirectories(
                            Path.of("%s/%s/".formatted(buildDir, buildPackage.replace(".", "/")))
                    ).resolve("schema_export.json"),
                    CREATE, WRITE)) {

                outputStream.write(gson.toJson(schemaDescriptors).getBytes());
            }
        }
    }

    public List<SchemaDescriptor> loadSchemaDescriptorsFromJson(String json) {
        Type schemaDescriptorListType = new TypeToken<List<SchemaDescriptor>>() {
        }.getType();
        return gson.fromJson(json, schemaDescriptorListType);
    }

    public void generateJava(String tablesJson) throws IOException {
        List<SchemaDescriptor> schemaDescriptors;
        schemaDescriptors = loadSchemaDescriptorsFromJson(tablesJson);
        for (SchemaDescriptor schemaDescriptor : schemaDescriptors) {
            LinkedHashMap<String, TypeSpec> tableClasses = new LinkedHashMap<>();

            for (TableDescriptor tableDescriptor : schemaDescriptor.tables()) {
                JavaFile storageServiceBaseClass = storageServiceBaseGenerator.generate(buildPackage, schemaDescriptor, tableDescriptor);
                storageServiceBaseClass.writeToPath(Path.of(buildDir));

                JavaFile storageModuleClass = storageModuleGenerator.generate(buildPackage, schemaDescriptor, tableDescriptor);
                storageModuleClass.writeToPath(Path.of(buildDir));

                TypeSpec tableClass = tableGenerator.generate(
                        buildPackage,
                        schemaDescriptor,
                        tableDescriptor,
                        tableDescriptor.columns()
                                       .stream()
                                       .map(columnDescriptor -> columnGenerator.generate(tableDescriptor, columnDescriptor))
                                       .collect(Collectors.toList()),
                        tableDescriptor.columns());

                tableClasses.put(tableDescriptor.getInstanceVariableName(), tableClass);
            }

            JavaFile.builder(buildPackage, schemaGenerator.generate(schemaDescriptor, tableClasses))
                    .build()
                    .writeToPath(Path.of(buildDir));
        }
    }

    public void generateProto(String tablesJson) throws IOException {
        List<SchemaDescriptor> schemaDescriptors;
        schemaDescriptors = loadSchemaDescriptorsFromJson(tablesJson);

        for (SchemaDescriptor schemaDescriptor : schemaDescriptors) {
            for (TableDescriptor tableDescriptor : schemaDescriptor.tables()) {
                try (OutputStream outputStream = Files.newOutputStream(
                        Files.createDirectories(
                                Path.of("%s/%s/".formatted(
                                        buildDir,
                                        buildPackage.replace(".", "/")
                                ))
                        ).resolve("%s_%s.proto".formatted(schemaDescriptor.name(), tableDescriptor.name())),
                        CREATE, WRITE)) {

                    outputStream.write(protoGenerator.generate(buildPackage, schemaDescriptor, tableDescriptor).getBytes());
                }
            }
        }
    }

    public static void main(String[] args) throws SQLException, IOException {
        ExportType exportType = ExportType.valueOf(args[1].toUpperCase());
        String buildDir = args[2];
        String buildPackage = args[3];
        String targetJson = args[4]; // either a list of schemas, or a dict-list of schemas and tables

        Injector injector = ModuleLoader.createInjector(new ExportModule(buildDir, buildPackage));
        Exporter exporter = injector.getInstance(Exporter.class);

        switch (exportType) {
            case JSON:
                exporter.exportJson(targetJson);
                return;
            case PROTO:
                exporter.generateProto(targetJson);
                return;
            case JAVA:
                exporter.generateJava(targetJson);
        }
    }
}
