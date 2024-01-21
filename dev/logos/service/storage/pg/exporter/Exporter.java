package dev.logos.service.storage.pg.exporter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.querydsl.sql.codegen.MetaDataExporter;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import dev.logos.module.module.DatabaseModule;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Exporter {
    private final CodeGenerator codeGenerator;
    private final String buildDir;
    private final String buildPackage;

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

    @Deprecated
    public void export() throws SQLException, IOException {
        Injector injector = Guice.createInjector(new DatabaseModule());
        DataSource dataSource = injector.getInstance(DataSource.class);

        MetaDataExporter exporter = new MetaDataExporter();
        exporter.setPackageName(buildPackage);
        exporter.setTargetFolder(new File(buildDir));
        exporter.setSchemaToPackage(true);
        exporter.export(dataSource.getConnection().getMetaData());
    }

    public static void writeProtoSources(String protoSrc, String protoPath, String protoFilename) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream("%s/%s".formatted(protoPath, protoFilename))) {
            fileOutputStream.write(protoSrc.getBytes());
        }
    }

    public static void writeProtoDescriptorSet(List<FileDescriptorProto> fileDescriptorProtos, String decriptorSetPath, String descriptorSetFilename) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream("%s/%s".formatted(decriptorSetPath, descriptorSetFilename))) {
            FileDescriptorSet
                    .newBuilder()
                    .addAllFile(fileDescriptorProtos)
                    .build()
                    .writeTo(fileOutputStream);
        }
    }

    public static void main(String[] args) throws SQLException, IOException {
        try (Connection connection = getConnection()) {
            String build_dir = args[2];
            String build_package = args[3];

            Exporter exporter = new Exporter(args[2], args[3], new CodeGenerator(build_dir, build_package));
            exporter.export();

            List<FileDescriptorProto> fileDescriptorProtos = new ArrayList<>();

            List<SchemaDescriptor> schemaDescriptors = SchemaDescriptor.extract(
                    connection,
                    new Gson().fromJson(args[4], new TypeToken<Map<String, List<String>>>() {
                    }.getType()));

            for (SchemaDescriptor schemaDescriptor : schemaDescriptors) {
                List<TypeSpec> tableClasses = new ArrayList<>();

                for (TableDescriptor tableDescriptor : schemaDescriptor.tables()) {
                    exporter.codeGenerator.makeStorageServiceBaseClass(schemaDescriptor, tableDescriptor);
                    exporter.codeGenerator.makeStorageModule(schemaDescriptor, tableDescriptor);

                    FileDescriptorProto resultFileDescriptorProto =
                            exporter.codeGenerator.makeResultProtoFileDescriptor(schemaDescriptor, tableDescriptor);

                    fileDescriptorProtos.add(resultFileDescriptorProto);

                    String descriptorPath = "%s/%s/%s/".formatted(build_dir,
                                                                  build_package.replace(".", "/"),
                                                                  schemaDescriptor.name());

                    try {
                        Files.createDirectories(Path.of(descriptorPath));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        writeProtoSources(exporter.codeGenerator.makeProtoService(schemaDescriptor, tableDescriptor),
                                          descriptorPath,
                                          "%s.proto".formatted(tableDescriptor.name()));
                        writeProtoDescriptorSet(List.of(resultFileDescriptorProto),
                                                descriptorPath,
                                                "%s.desc".formatted(tableDescriptor.name()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    List<TypeSpec> columnClasses = new ArrayList<>();

                    for (ColumnDescriptor columnDescriptor : tableDescriptor.columns()) {
                        columnClasses.add(exporter.codeGenerator.makeColumnClass(tableDescriptor, columnDescriptor));
                    }

                    TypeSpec tableClass = exporter.codeGenerator.makeTableClass(
                            schemaDescriptor,
                            tableDescriptor,
                            columnClasses,
                            tableDescriptor.columns());

                    tableClasses.add(tableClass);
                }

                File protoDescriptorFile = new File(args[5]);
                writeProtoDescriptorSet(fileDescriptorProtos, protoDescriptorFile.getParent(), protoDescriptorFile.getName());

                JavaFile.builder(exporter.buildPackage,
                                 exporter.codeGenerator.makeSchemaClass(schemaDescriptor, tableClasses))
                        .build().writeToPath(Path.of(exporter.buildDir));
            }
        }
    }
}
