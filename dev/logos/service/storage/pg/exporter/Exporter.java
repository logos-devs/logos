package dev.logos.service.storage.pg.exporter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import dev.logos.service.storage.module.DatabaseModule;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;


public class Exporter {
    private final String buildDir;
    private final String buildPackage;
    private final CodeGenerator codeGenerator;
    private final DataSource dataSource;

    @Inject
    public Exporter(
            @BuildDir String buildDir,
            @BuildPackage String buildPackage,
            CodeGenerator codeGenerator,
            DataSource dataSource

    ) {
        this.buildDir = buildDir;
        this.buildPackage = buildPackage;
        this.codeGenerator = codeGenerator;
        this.dataSource = dataSource;
    }

    private enum ExportType {
        JSON,
        PROTO,
        JAVA
    }

    public String dumpSchemaDescriptorsToJson(List<SchemaDescriptor> schemaDescriptors) {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(schemaDescriptors);
    }

    public List<SchemaDescriptor> loadSchemaDescriptorsFromJson(String json) {
        Gson gson = new Gson();
        Type schemaDescriptorListType = new TypeToken<List<SchemaDescriptor>>() {
        }.getType();
        return gson.fromJson(json, schemaDescriptorListType);
    }

    public void generateJava(String tablesJson) throws IOException {
        List<SchemaDescriptor> schemaDescriptors;
        schemaDescriptors = loadSchemaDescriptorsFromJson(tablesJson);
        for (SchemaDescriptor schemaDescriptor : schemaDescriptors) {
            List<TypeSpec> tableClasses = new ArrayList<>();

            for (TableDescriptor tableDescriptor : schemaDescriptor.tables()) {
                codeGenerator.makeStorageServiceBaseClass(schemaDescriptor, tableDescriptor);
                codeGenerator.makeStorageModule(schemaDescriptor, tableDescriptor);

                TypeSpec tableClass = codeGenerator.makeTableClass(
                        schemaDescriptor,
                        tableDescriptor,
                        tableDescriptor.columns()
                                       .stream()
                                       .map(columnDescriptor -> codeGenerator.makeColumnClass(tableDescriptor, columnDescriptor))
                                       .collect(Collectors.toList()),
                        tableDescriptor.columns());

                tableClasses.add(tableClass);
            }

            JavaFile.builder(buildPackage,
                             codeGenerator.makeSchemaClass(schemaDescriptor, tableClasses))
                    .build().writeToPath(Path.of(buildDir));
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

                    outputStream.write(codeGenerator.makeProtoService(schemaDescriptor, tableDescriptor).getBytes());
                }
            }
        }
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

                outputStream.write(dumpSchemaDescriptorsToJson(schemaDescriptors).getBytes());
            }
        }
    }

    public static void main(String[] args) throws SQLException, IOException {
        ExportType exportType = ExportType.valueOf(args[1].toUpperCase());
        String buildDir = args[2];
        String buildPackage = args[3];
        String targetJson = args[4]; // either a list of schemas, or a dict-list of schemas and tables

        Injector injector = Guice.createInjector(new DatabaseModule(), new ExportModule(buildDir, buildPackage));
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
