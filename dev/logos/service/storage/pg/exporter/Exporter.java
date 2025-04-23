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
import dev.logos.service.storage.pg.exporter.codegen.proto.QualifierProtoGenerator;
import dev.logos.service.storage.pg.exporter.codegen.qualifier.QualifierGenerator;
import dev.logos.service.storage.pg.exporter.codegen.schema.SchemaGenerator;
import dev.logos.service.storage.pg.exporter.codegen.service.StorageServiceBaseGenerator;
import dev.logos.service.storage.pg.exporter.codegen.storage.TableStorageGenerator;
import dev.logos.service.storage.pg.exporter.codegen.table.TableGenerator;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierDescriptor;
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
    private final QualifierProtoGenerator qualifierProtoGenerator;
    private final QualifierGenerator qualifierGenerator;
    private final StorageServiceBaseGenerator storageServiceBaseGenerator;
    private final StorageModuleGenerator storageModuleGenerator;
    private final TableStorageGenerator tableStorageGenerator;
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
            QualifierProtoGenerator qualifierProtoGenerator,
            QualifierGenerator qualifierGenerator,
            StorageServiceBaseGenerator storageServiceBaseGenerator,
            StorageModuleGenerator storageModuleGenerator,
            TableStorageGenerator tableStorageGenerator
    ) {
        this.buildDir = buildDir;
        this.buildPackage = buildPackage;
        this.dataSource = dataSource;
        this.schemaGenerator = schemaGenerator;
        this.tableGenerator = tableGenerator;
        this.columnGenerator = columnGenerator;
        this.protoGenerator = protoGenerator;
        this.qualifierProtoGenerator = qualifierProtoGenerator;
        this.qualifierGenerator = qualifierGenerator;
        this.storageServiceBaseGenerator = storageServiceBaseGenerator;
        this.storageModuleGenerator = storageModuleGenerator;
        this.tableStorageGenerator = tableStorageGenerator;
    }

    private enum ExportType {
        JSON,
        PROTO,
        JAVA
    }

    public void exportJson(String tablesJson) throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection()) {
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

                JavaFile tableStorageClass = tableStorageGenerator.generate(buildPackage, schemaDescriptor, tableDescriptor);
                tableStorageClass.writeToPath(Path.of(buildDir));

                TypeSpec tableClass = tableGenerator.generate(
                        buildPackage,
                        schemaDescriptor,
                        tableDescriptor,
                        tableDescriptor.columns()
                                       .stream()
                                       .map(columnDescriptor -> columnGenerator.generate(tableDescriptor, columnDescriptor))
                                       .collect(Collectors.toList()),
                        tableDescriptor.columns(),
                        tableDescriptor.qualifierDescriptors()
                                       .stream()
                                       .map(qualifierDescriptor -> qualifierGenerator.generate(tableDescriptor, qualifierDescriptor))
                                       .collect(Collectors.toList())
                );

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

        // Generate Proto and Java files for each table
        for (SchemaDescriptor schemaDescriptor : schemaDescriptors) {
            for (TableDescriptor tableDescriptor : schemaDescriptor.tables()) {
                Path protoPath = Files.createDirectories(
                        Path.of("%s/%s/".formatted(
                                buildDir,
                                buildPackage.replace(".", "/")
                        ))
                ).resolve("%s_%s.proto".formatted(schemaDescriptor.name(), tableDescriptor.name()));

                // Generate standard Proto content
                String standardProto = protoGenerator.generate(buildPackage, schemaDescriptor, tableDescriptor);

                // Check if table has qualifiers
                List<QualifierDescriptor> qualifiers = tableDescriptor.qualifierDescriptors();

                if (!qualifiers.isEmpty()) {
                    // Generate qualifier message definitions
                    String qualifierMessages = qualifierProtoGenerator.generateQualifierMessages(qualifiers);

                    // Generate qualifier fields for request types
                    String listQualifiers = qualifierProtoGenerator.generateQualifierFields("List", qualifiers);
                    String updateQualifiers = qualifierProtoGenerator.generateQualifierFields("Update", qualifiers);
                    String deleteQualifiers = qualifierProtoGenerator.generateQualifierFields("Delete", qualifiers);

                    // Insert qualifier messages before service definition
                    standardProto = insertQualifierContent(standardProto, qualifierMessages);

                    // Add qualifier fields to request messages
                    standardProto = addQualifierFieldsToMessage(standardProto,
                            "List" + tableDescriptor.getClassName().simpleName() + "Request", listQualifiers);
                    standardProto = addQualifierFieldsToMessage(standardProto,
                            "Update" + tableDescriptor.getClassName().simpleName() + "Request", updateQualifiers);
                    standardProto = addQualifierFieldsToMessage(standardProto,
                            "Delete" + tableDescriptor.getClassName().simpleName() + "Request", deleteQualifiers);
                }

                // Write Proto file
                try (OutputStream outputStream = Files.newOutputStream(protoPath, CREATE, WRITE)) {
                    outputStream.write(standardProto.getBytes());
                }

                // Generate Java service base class
                generateServiceBaseClass(buildPackage, schemaDescriptor, tableDescriptor);
            }
        }
    }

    /**
     * Insert qualifier message definitions before service definition.
     */
    private String insertQualifierContent(String protoContent, String qualifierMessages) {
        if (qualifierMessages == null || qualifierMessages.isEmpty()) {
            return protoContent;
        }

        // Find the service definition
        int serviceIndex = protoContent.lastIndexOf("service ");
        if (serviceIndex > 0) {
            // Insert qualifier messages before service
            return protoContent.substring(0, serviceIndex) +
                    qualifierMessages + "\n\n" +
                    protoContent.substring(serviceIndex);
        }

        return protoContent;
    }

    /**
     * Add qualifier fields to a specific message definition.
     */
    private String addQualifierFieldsToMessage(String protoContent, String messageName, String qualifierFields) {
        if (qualifierFields == null || qualifierFields.isEmpty()) {
            return protoContent;
        }

        // Find the message definition
        String messageStart = "message " + messageName + " {";
        int messageIndex = protoContent.indexOf(messageStart);
        if (messageIndex < 0) {
            return protoContent;
        }

        // Find the closing brace of the message
        int startPos = messageIndex + messageStart.length();
        int braceLevel = 1;
        int endIndex = -1;

        for (int i = startPos; i < protoContent.length(); i++) {
            char c = protoContent.charAt(i);
            if (c == '{') {
                braceLevel++;
            } else if (c == '}') {
                braceLevel--;
                if (braceLevel == 0) {
                    endIndex = i;
                    break;
                }
            }
        }

        if (endIndex > 0) {
            // Insert qualifier fields before closing brace
            return protoContent.substring(0, endIndex) +
                    "\n" + qualifierFields +
                    protoContent.substring(endIndex);
        }

        return protoContent;
    }

    /**
     * Generate Java base service class for a table.
     */
    private void generateServiceBaseClass(
            String targetPackage,
            SchemaDescriptor schemaDescriptor,
            TableDescriptor tableDescriptor) throws IOException {
        JavaFile serviceBaseFile = storageServiceBaseGenerator.generate(
                targetPackage, schemaDescriptor, tableDescriptor);

        Path javaPath = Files.createDirectories(
                Path.of("%s/%s/".formatted(
                        buildDir,
                        targetPackage.replace(".", "/")
                )));

        serviceBaseFile.writeTo(javaPath);
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