package dev.logos.service.storage.pg.exporter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import dev.logos.service.storage.module.DatabaseModule;

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
    private final CodeGenerator codeGenerator;
    private final String buildDir;
    private final String buildPackage;

    private enum ExportType {
        JSON,
        PROTO,
        JAVA
    }

    Exporter(String buildDir,
             String buildPackage,
             CodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
        this.buildDir = buildDir;
        this.buildPackage = buildPackage;
    }

    public static Connection getConnection() throws SQLException {
        Injector injector = Guice.createInjector(new DatabaseModule());
        DataSource dataSource = injector.getInstance(DataSource.class);
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        return connection;
    }

    public static String dumpSchemaDescriptorsToJson(List<SchemaDescriptor> schemaDescriptors) {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(schemaDescriptors);
    }

    public static List<SchemaDescriptor> loadSchemaDescriptorsFromJson(String json) {
        Gson gson = new Gson();
        Type schemaDescriptorListType = new TypeToken<List<SchemaDescriptor>>() {}.getType();
        return gson.fromJson(json, schemaDescriptorListType);
    }

    public static void main(String[] args) throws SQLException, IOException {
        try (Connection connection = getConnection()) {
            ExportType exportType = ExportType.valueOf(args[1].toUpperCase());
            String build_dir = args[2];
            String build_package = args[3];
            String tablesJson = args[4];

            Exporter exporter = new Exporter(build_dir, build_package, new CodeGenerator(build_dir, build_package));
            List<SchemaDescriptor> schemaDescriptors;

            switch (exportType) {
                case JSON:
                    schemaDescriptors = SchemaDescriptor.extract(
                            connection,
                            new Gson().fromJson(tablesJson, new TypeToken<Map<String, List<String>>>() {
                            }.getType()));

                    try (OutputStream outputStream = Files.newOutputStream(
                            Files.createDirectories(
                                    Path.of("%s/%s/".formatted(build_dir, build_package.replace(".", "/")))
                            ).resolve("schema_export.json"),
                            CREATE, WRITE)) {

                        outputStream.write(dumpSchemaDescriptorsToJson(schemaDescriptors).getBytes());
                    }
                    return;
                case PROTO:
                    schemaDescriptors = loadSchemaDescriptorsFromJson(tablesJson);

                    for (SchemaDescriptor schemaDescriptor : schemaDescriptors) {
                        for (TableDescriptor tableDescriptor : schemaDescriptor.tables()) {
                            try (OutputStream outputStream = Files.newOutputStream(
                                    Files.createDirectories(
                                            Path.of("%s/%s/%s/".formatted(
                                                    build_dir,
                                                    build_package.replace(".", "/"),
                                                    schemaDescriptor.name()
                                            ))
                                    ).resolve("%s.proto".formatted(tableDescriptor.name())),
                                    CREATE, WRITE)) {

                                outputStream.write(exporter.codeGenerator.makeProtoService(schemaDescriptor, tableDescriptor).getBytes());
                            }
                        }
                    }

                    break;
                case JAVA:
                    schemaDescriptors = loadSchemaDescriptorsFromJson(tablesJson);
                    for (SchemaDescriptor schemaDescriptor : schemaDescriptors) {
                        List<TypeSpec> tableClasses = new ArrayList<>();

                        for (TableDescriptor tableDescriptor : schemaDescriptor.tables()) {
                            exporter.codeGenerator.makeStorageServiceBaseClass(schemaDescriptor, tableDescriptor);
                            exporter.codeGenerator.makeStorageModule(schemaDescriptor, tableDescriptor);

                            TypeSpec tableClass = exporter.codeGenerator.makeTableClass(
                                    schemaDescriptor,
                                    tableDescriptor,
                                    tableDescriptor.columns().stream().map(
                                                    columnDescriptor -> exporter.codeGenerator.makeColumnClass(tableDescriptor, columnDescriptor))
                                            .collect(Collectors.toList()),
                                    tableDescriptor.columns());

                            tableClasses.add(tableClass);
                        }

                        JavaFile.builder(exporter.buildPackage,
                                        exporter.codeGenerator.makeSchemaClass(schemaDescriptor, tableClasses))
                                .build().writeToPath(Path.of(exporter.buildDir));
                    }
                    break;
            }
        }
    }
}
