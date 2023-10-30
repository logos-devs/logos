package dev.logos.stack.service.storage.exporter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.*;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.querydsl.sql.codegen.MetaDataExporter;
import com.squareup.javapoet.*;
import dev.logos.stack.module.DatabaseModule;
import dev.logos.stack.service.storage.EntityStorage;
import dev.logos.stack.service.storage.EntityStorageService;
import dev.logos.stack.service.storage.TableStorage;
import dev.logos.stack.service.storage.pg.Column;
import dev.logos.stack.service.storage.pg.Identifier;
import dev.logos.stack.service.storage.pg.Relation;
import dev.logos.stack.service.storage.pg.Schema;
import io.grpc.stub.StreamObserver;

import javax.lang.model.element.Modifier;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static dev.logos.stack.service.storage.pg.Identifier.snakeToCamelCase;
import static javax.lang.model.element.Modifier.*;


record ColumnDescriptor(String name, String type) {

    public Type getProtobufType() {
        return switch (this.type) {
            case "smallint", "integer" -> Type.TYPE_SINT32;
            case "bigint" -> Type.TYPE_SINT64;
            case "real" -> Type.TYPE_FLOAT;
            case "double precision" -> Type.TYPE_DOUBLE;
            case "numeric", "decimal" -> Type.TYPE_FIXED64;
            case "char",
                    "varchar",
                    "character varying",
                    "text",
                    "text[]",
                    "timestamp",
                    "timestamp with time zone",
                    "date" -> Type.TYPE_STRING;
            case "bytea", "uuid" -> Type.TYPE_BYTES;
            case "boolean" -> Type.TYPE_BOOL;
            default -> throw new IllegalArgumentException("Unsupported type: " + this.type);
        };
    }

    public String getJavaCast() {
        return switch (this.type) {
            case "ext[]" -> "(String[])";
            default -> "";
        };
    }

    public Boolean isArray() {
        return this.type.endsWith("[]");
    }

    public String getResultSetMethod() {
        Type protobufType = getProtobufType();
        if (isArray()) {
            return "getArray";
        } else {
            return switch (protobufType) {
                case TYPE_BOOL -> "getBoolean";
                case TYPE_BYTES -> "getBytes";
                case TYPE_DOUBLE,
                        TYPE_FIXED64 -> "getDouble";
                case TYPE_FLOAT -> "getFloat";
                case TYPE_SINT32 -> "getInt";
                case TYPE_SINT64 -> "getLong";
                case TYPE_STRING -> "getString";
                default -> throw new RuntimeException("Unknown type: " + protobufType);
            };
        }
    }

    public String getProtobufFieldSetter() {
        String setterName = snakeToCamelCase(this.name);
        String setterPrefix = isArray() ? "addAll" : "set";
        return "%s%s%s".formatted(
                setterPrefix,
                setterName.substring(0, 1).toUpperCase(),
                setterName.substring(1)
        );
    }

    CodeBlock convertType(CodeBlock innerCall) {
        Type protobufType = getProtobufType();
        if (isArray()) {
            return CodeBlock.of("$T.asList((String[])$L.getArray())", Arrays.class, innerCall);
        } else {
            return switch (protobufType) {
                case TYPE_BOOL,
                        TYPE_DOUBLE,
                        TYPE_FIXED64,
                        TYPE_SINT32,
                        TYPE_SINT64,
                        TYPE_STRING -> innerCall;
                case TYPE_BYTES -> CodeBlock.of("$T.copyFrom($L)", ByteString.class, innerCall);
                case TYPE_FLOAT -> CodeBlock.of("%L.floatValue()", innerCall);
                default -> throw new RuntimeException("Unknown type: " + protobufType);
            };
        }
    }
}

public class Exporter {

    private final Connection connection;

    Exporter(Connection connection,
             String build_dir,
             String build_package) {
        this.connection = connection;
        this.build_dir = build_dir;
        this.build_package = build_package;
    }

    private static final Modifier[] INNER_CLASS_MODIFIERS = new Modifier[]{PUBLIC, STATIC};
    private static final Modifier[] VARIABLE_MODIFIERS = new Modifier[]{PUBLIC, STATIC, FINAL};
    private final String build_dir;
    private static final String[] JAVA_KEYWORDS = {"abstract", "continue", "for", "new", "switch", "assert", "default",
            "goto", "package", "synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements",
            "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum", "instanceof", "return",
            "transient", "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class",
            "finally", "long", "strictfp", "volatile", "const", "float", "native", "super", "while"};
    private final String build_package;

    public void export() throws SQLException, IOException {
        Injector injector = Guice.createInjector(new DatabaseModule());
        DataSource dataSource = injector.getInstance(DataSource.class);

        MetaDataExporter exporter = new MetaDataExporter();
        exporter.setPackageName(build_package);
        exporter.setTargetFolder(new File(build_dir));
        exporter.setSchemaToPackage(true);
        exporter.export(dataSource.getConnection().getMetaData());
    }

    public static String classNameToInstanceName(String className) {
        String instanceName = className.substring(0, 1).toLowerCase() + className.substring(1);
        if (Arrays.asList(JAVA_KEYWORDS).contains(instanceName)) {
            instanceName = "_" + instanceName;
        }
        return instanceName;
    }

    private TypeSpec makeSchemaClass(ClassName schemaClassName,
                                     String schemaIdentifier,
                                     Iterable<TypeSpec> tableClasses) {

        TypeSpec.Builder schemaClassBuilder = TypeSpec.classBuilder(schemaClassName)
                .addModifiers(PUBLIC)
                .superclass(Schema.class)
                .addMethod(MethodSpec.constructorBuilder()
                        .addStatement("super($S, $S)", schemaIdentifier, Identifier.quoteIdentifier(schemaIdentifier))
                        .build())
                .addTypes(tableClasses);

        for (TypeSpec tableClass : tableClasses) {
            ClassName tableClassName = ClassName.bestGuess(tableClass.name);
            schemaClassBuilder.addField(
                    FieldSpec.builder(tableClassName,
                                    classNameToInstanceName(tableClass.name),
                                    VARIABLE_MODIFIERS)
                            .initializer("new $T()", tableClassName)
                            .build());
        }

        return schemaClassBuilder.build();
    }

    private void makeResultProto(String schemaIdentifier,
                                   String tableIdentifier,
                                   ClassName tableClassName,
                                   List<ColumnDescriptor> columnDescriptors) {
        String descriptorPath = "%s/%s/%s/".formatted(build_dir, build_package.replace(".", "/"), schemaIdentifier);
        String descriptorFilename = "%s%s.desc".formatted(descriptorPath, tableIdentifier);

        try {
            Files.createDirectories(Path.of(descriptorPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(descriptorFilename)) {
            FileDescriptorSet
                    .newBuilder()
                    .addFile(
                            FileDescriptorProto
                                    .newBuilder()
                                    .setName(tableIdentifier + ".proto")
                                    .setSyntax("proto3")
                                    .setOptions(
                                            FileOptions
                                                    .newBuilder()
                                                    .setJavaPackage(build_package + "." + schemaIdentifier)
                                                    .setJavaMultipleFiles(true)
                                                    .build())
                                    .addMessageType(
                                            DescriptorProto
                                                    .newBuilder()
                                                    .setName("List" + tableClassName.simpleName() + "Request")
                                                    .addField(
                                                            FieldDescriptorProto
                                                                    .newBuilder()
                                                                    .setName("limit")
                                                                    .setType(Type.TYPE_INT64)
                                                                    .setNumber(1)
                                                                    .build())
                                                    .addField(
                                                            FieldDescriptorProto
                                                                    .newBuilder()
                                                                    .setName("offset")
                                                                    .setType(Type.TYPE_INT64)
                                                                    .setNumber(2)
                                                                    .build())
                                                    .build())
                                    .addMessageType(
                                            DescriptorProto
                                                    .newBuilder()
                                                    .setName("List" + tableClassName.simpleName() + "Response")
                                                    .addField(
                                                            FieldDescriptorProto
                                                                    .newBuilder()
                                                                    .setName("results")
                                                                    .setType(Type.TYPE_MESSAGE)
                                                                    .setTypeName(tableClassName.simpleName())
                                                                    .setLabel(Label.LABEL_REPEATED)
                                                                    .setNumber(1)
                                                                    .build())
                                                    .build())
                                    .addMessageType(
                                            DescriptorProto
                                                    .newBuilder()
                                                    .setName(tableClassName.simpleName())
                                                    .addAllField(
                                                            IntStream.range(0, columnDescriptors.size())
                                                                    .mapToObj(i -> {
                                                                        ColumnDescriptor columnDescriptor = columnDescriptors.get(i);
                                                                        FieldDescriptorProto.Builder fieldDescriptorProto = FieldDescriptorProto
                                                                                .newBuilder()
                                                                                .setName(columnDescriptor.name())
                                                                                .setType(columnDescriptor.getProtobufType())
                                                                                .setNumber(i + 1);

                                                                        if (columnDescriptor.isArray()) {
                                                                            fieldDescriptorProto.setLabel(Label.LABEL_REPEATED);
                                                                        }

                                                                        return fieldDescriptorProto.build();
                                                                    }).toList())
                                                    .build())
                                    .addService(
                                            ServiceDescriptorProto
                                                    .newBuilder()
                                                    .setName(tableClassName.simpleName() + "StorageService")
                                                    .addMethod(
                                                            MethodDescriptorProto
                                                                    .newBuilder()
                                                                    .setName("List")
                                                                    .setInputType("List" + tableClassName.simpleName() + "Request")
                                                                    .setOutputType("List" + tableClassName.simpleName() + "Response")
                                                                    .build())
                                                    .build()
                                    )
                    )
                    .build()
                    .writeTo(fileOutputStream);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TypeSpec makeTableClass(ClassName schemaClassName,
                                    String schemaIdentifier,
                                    String tableIdentifier,
                                    ClassName tableClassName,
                                    String tableInstanceVariableName,
                                    Iterable<TypeSpec> columnClasses,
                                    List<ColumnDescriptor> columnDescriptors) throws IOException {

        makeResultProto(
                schemaIdentifier,
                tableIdentifier,
                tableClassName,
                columnDescriptors);

        ClassName resultProtoClassName =
                ClassName.get(build_package + "." + schemaIdentifier, tableClassName.simpleName());

        makeStorageServiceBaseClass(
                schemaClassName,
                schemaIdentifier,
                tableClassName,
                tableInstanceVariableName);

        makeStorageModule(
                schemaClassName,
                schemaIdentifier,
                tableClassName,
                tableIdentifier,
                tableInstanceVariableName);

        TypeSpec.Builder tableClassBuilder = TypeSpec
                .classBuilder(tableClassName)
                .addModifiers(INNER_CLASS_MODIFIERS)
                .superclass(Relation.class)
                .addMethod(MethodSpec.constructorBuilder()
                        .addStatement("super($S, $S)",
                                tableIdentifier,
                                Identifier.quoteIdentifier(schemaIdentifier) + "." + Identifier.quoteIdentifier(tableIdentifier))
                        .build())
                .addMethod(
                        MethodSpec
                                .methodBuilder("toProtobuf")
                                .addModifiers(PUBLIC)
                                .addException(SQLException.class)
                                .addParameter(ResultSet.class, "resultSet")
                                .returns(resultProtoClassName)
                                .addStatement(
                                        CodeBlock.builder()
                                                .add("$T.Builder builder = $T.newBuilder();\n", resultProtoClassName, resultProtoClassName)
                                                .add(columnDescriptors
                                                        .stream()
                                                        .map(columnDescriptor -> {
                                                            String columnName = columnDescriptor.name();
                                                            return CodeBlock.of("if (resultSet.getObject($S) != null) { builder.$N($L); }\n",
                                                                    columnName,
                                                                    columnDescriptor.getProtobufFieldSetter(),
                                                                    columnDescriptor.convertType(
                                                                            CodeBlock.of(
                                                                                    "%sresultSet.$N($S)".formatted(columnDescriptor.getJavaCast()),
                                                                                    columnDescriptor.getResultSetMethod(),
                                                                                    columnName)));
                                                        }).collect(CodeBlock.joining(";")))
                                                .add("return builder.build()")
                                                .build())
                                .build())
                .addTypes(columnClasses);

        for (TypeSpec columnClass : columnClasses) {
            ClassName columnClassName = ClassName.bestGuess(columnClass.name);
            tableClassBuilder.addField(
                    FieldSpec.builder(columnClassName,
                                    classNameToInstanceName(columnClass.name),
                                    VARIABLE_MODIFIERS)
                            .initializer("new $T()", columnClassName)
                            .build());
        }

        return tableClassBuilder.build();
    }

    private void makeStorageServiceBaseClass(
            ClassName schemaClassName,
            String schemaIdentifier,
            ClassName tableClassName,
            String tableInstanceVariableName
    ) throws IOException {
        String packageName = build_package + "." + schemaIdentifier;
        ClassName entityClassName = ClassName.bestGuess(String.format("%s.%s", packageName, tableClassName));
        ParameterizedTypeName entityStorageClass = ParameterizedTypeName.get(
                ClassName.get(EntityStorage.class),
                entityClassName
        );

        JavaFile.builder(packageName,
                        TypeSpec.classBuilder(String.format("%sStorageServiceBase", tableClassName))
                                .addModifiers(PUBLIC, ABSTRACT)
                                .superclass(ClassName.bestGuess(String.format("%s.%sStorageServiceGrpc.%sStorageServiceImplBase", packageName, tableClassName, tableClassName)))
                                .addField(FieldSpec.builder(entityStorageClass, "storage", PRIVATE)
                                        .addAnnotation(Inject.class)
                                        .build())
                                .addSuperinterface(ParameterizedTypeName.get(
                                        ClassName.get(EntityStorageService.class),
                                        ClassName.bestGuess(String.format("%s.List%sRequest", packageName, tableClassName)),
                                        ClassName.bestGuess(String.format("%s.List%sResponse", packageName, tableClassName)),
                                        ClassName.bestGuess(String.format("%s.%s", packageName, tableClassName)) // ,
                                ))
                                .addMethod(MethodSpec.methodBuilder("getStorage")
                                        .addAnnotation(Override.class)
                                        .addModifiers(PUBLIC)
                                        .returns(entityStorageClass)
                                        .addStatement("return this.storage")
                                        .build())
//                                .addMethod(MethodSpec.methodBuilder("getStorage")
//                                        .addAnnotation(Override.class)
//                                        .addModifiers(Modifier.PUBLIC)
//                                        .returns(ParameterizedTypeName.get(
//                                                ClassName.get(EntityStorage.class),
//                                                entityClassName
//                                        ))
//                                        .addStatement(
//                                                String.format("return new $T<$T, $T>(%s, $T.class, $T.class)", tableInstanceVariableName),
//                                                TableStorage.class,
//                                                ClassName.bestGuess(String.format("%s.%s", packageName, tableClassName)),
//                                                ClassName.get(UUID.class),
//                                                tableClassName,
//                                                ClassName.get(UUID.class))
//                                        .build())
                                .addMethod(MethodSpec.methodBuilder("list")
                                        .addAnnotation(Override.class)
                                        .addModifiers(PUBLIC)
                                        .addParameter(ClassName.bestGuess(String.format("%s.List%sRequest", packageName, tableClassName)),
                                                "request")
                                        .addParameter(ParameterizedTypeName.get(ClassName.get(StreamObserver.class),
                                                        ClassName.bestGuess(String.format("%s.List%sResponse", packageName, tableClassName))),
                                                "responseObserver")
                                        .addStatement("listHandler(request, responseObserver)")
                                        .build())
                                .addMethod(MethodSpec.methodBuilder("result")
                                        .addAnnotation(Override.class)
                                        .addModifiers(PUBLIC)
                                        .addParameter(ParameterizedTypeName.get(ClassName.get(Stream.class),
                                                        ClassName.bestGuess(String.format("%s.%s", packageName, tableClassName))),
                                                String.format("%sListStream", tableInstanceVariableName))
                                        .returns(ClassName.bestGuess(String.format("%s.List%sResponse", packageName, tableClassName)))
                                        .addStatement(
                                                String.format("return $T.newBuilder().addAllResults(%sListStream.toList()).build()", tableInstanceVariableName),
                                                ClassName.bestGuess(String.format("%s.List%sResponse", packageName, tableClassName)))
                                        .build())
                                .build()
                )
                .addStaticImport(ClassName.bestGuess(String.format("%s.%s", build_package, schemaClassName)),
                        tableInstanceVariableName)
                .build()
                .writeToPath(Path.of(build_dir));
    }

    private void makeStorageModule(
            ClassName schemaClassName,
            String schemaIdentifier,
            ClassName tableClassName,
            String tableIdentifier,
            String tableInstanceVariableName
    ) throws IOException {
        String packageName = build_package + "." + schemaIdentifier + "." + tableIdentifier;

        ClassName entityClassName = ClassName.bestGuess(build_package + "." + schemaIdentifier + "." + tableClassName);

        JavaFile.builder(packageName,
                        TypeSpec.classBuilder("StorageModule")
                                .addModifiers(PUBLIC)
                                .superclass(AbstractModule.class)
                                .addMethod(MethodSpec.methodBuilder("configure")
                                        .addAnnotation(Override.class)
                                        .addModifiers(PROTECTED)
                                        .addStatement(
                                                String.format("bind(new $T(){}).toInstance(new $T(%s, $T.class, $T.class))", tableInstanceVariableName),
                                                ParameterizedTypeName.get(
                                                        ClassName.get(TypeLiteral.class),
                                                        ParameterizedTypeName.get(
                                                                ClassName.get(EntityStorage.class),
                                                                entityClassName)
                                                ),
                                                ParameterizedTypeName.get(
                                                        ClassName.get(TableStorage.class),
                                                        entityClassName,
                                                        ClassName.get(UUID.class)
                                                ),
                                                entityClassName,
                                                ClassName.get(UUID.class))
                                        .addStatement("super.configure()")
                                        .build())
                                .build()
                )
                .addStaticImport(ClassName.bestGuess(String.format("%s.%s", build_package, schemaClassName)),
                        tableInstanceVariableName)
                .build()
                .writeToPath(Path.of(build_dir));
    }

    private TypeSpec makeColumnClass(String tableIdentifier,
                                     ColumnDescriptor columnDescriptor) {
        String columnIdentifier = columnDescriptor.name();
        String columnType = columnDescriptor.type();

        StringBuilder columnPostfix = new StringBuilder();
        while (Objects.equals(columnIdentifier + columnPostfix, tableIdentifier)) {
            columnPostfix.append("_");
        }
        ClassName columnClassName = ClassName.bestGuess(
                snakeToCamelCase(columnIdentifier) + columnPostfix);

        return TypeSpec.classBuilder(columnClassName)
                .addModifiers(INNER_CLASS_MODIFIERS)
                .superclass(Column.class)
                .addMethod(MethodSpec.methodBuilder("toProtobuf")
                        .addParameter(String.class, "dbValue")
                        .addStatement("System.out.println(\"toProtobufType\")")
                        .addStatement("return $S", "test")
                        .returns(String.class)
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addStatement("super($S, $S, $S)",
                                columnIdentifier,
                                Identifier.quoteIdentifier(tableIdentifier) + '.'
                                        + Identifier.quoteIdentifier(columnIdentifier),
                                columnDescriptor.type())
                        .build())
                .build();
    }

    private List<String> getSchemaIdentifiers(Set<String> selectedSchemas) throws SQLException {
        List<String> schemaIdentifiers = new ArrayList<>();

        for (String schema : selectedSchemas) {
            ResultSet schemaResultSet =
                    connection.createStatement().executeQuery(
                            "select schema_name from information_schema.schemata where schema_name = '"
                                    + schema + "'");

            if (!schemaResultSet.next()) {
                throw new IllegalArgumentException("Schema " + schema + " does not exist");
            }

            schemaIdentifiers.add(schemaResultSet.getString("schema_name"));
        }

        return schemaIdentifiers;
    }

    private List<String> getTableIdentifiers(String schemaIdentifier,
                                             Map<String, List<String>> selectedTables) {
        List<String> tableIdentifiers = new ArrayList<>();
        for (String table : selectedTables.get(schemaIdentifier)) {
            try {
                PreparedStatement tableQueryStmt = connection.prepareStatement(
                        "select table_name from information_schema.tables where table_schema = ? and table_name = ?");
                tableQueryStmt.setString(1, schemaIdentifier);
                tableQueryStmt.setString(2, table);
                ResultSet tableResultSet = tableQueryStmt.executeQuery();

                if (!tableResultSet.next()) {
                    throw new IllegalArgumentException("Table " + table + " does not exist");
                }

                tableIdentifiers.add(tableResultSet.getString("table_name"));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return tableIdentifiers;
    }

    private List<ColumnDescriptor> getColumnDescriptors(String schemaIdentifier,
                                                        String tableIdentifier) {
        try {
            PreparedStatement columnQueryStmt = connection.prepareStatement(
                    "select column_name, udt_name::regtype::text as data_type from information_schema.columns where table_schema = ? and table_name = ?");
            columnQueryStmt.setString(1, schemaIdentifier);
            columnQueryStmt.setString(2, tableIdentifier);
            ResultSet columnResultSet = columnQueryStmt.executeQuery();

            List<ColumnDescriptor> columnDescriptors = new ArrayList<>();
            while (columnResultSet.next()) {
                columnDescriptors.add(
                        new ColumnDescriptor(
                                columnResultSet.getString("column_name"),
                                columnResultSet.getString("data_type")
                        ));
            }
            return columnDescriptors;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void extractSchema(String schemaIdentifier,
                               Map<String, List<String>> selectedTables) throws IOException {

        ClassName schemaClassName = ClassName.bestGuess(snakeToCamelCase(schemaIdentifier));
        JavaFile.builder(
                build_package,
                makeSchemaClass(schemaClassName, schemaIdentifier, getTableIdentifiers(schemaIdentifier, selectedTables)
                        .stream()
                        .map(tableIdentifier -> {
                            List<ColumnDescriptor> columnDescriptors = getColumnDescriptors(
                                    schemaIdentifier, tableIdentifier);

                            /* avoid collision with schema name */
                            StringBuilder tablePostfix = new StringBuilder();
                            while (Objects.equals(tableIdentifier + tablePostfix, schemaIdentifier)) {
                                tablePostfix.append("_");
                            }
                            ClassName tableClassName = ClassName.bestGuess(snakeToCamelCase(tableIdentifier) + tablePostfix);
                            String tableInstanceVariableName = classNameToInstanceName(
                                    tableClassName.simpleName() + tablePostfix);

                            try {
                                return makeTableClass(
                                        schemaClassName,
                                        schemaIdentifier,
                                        tableIdentifier,
                                        tableClassName,
                                        tableInstanceVariableName,
                                        getColumnDescriptors(schemaIdentifier, tableIdentifier)
                                                .stream()
                                                .map(columnDescriptor ->
                                                        makeColumnClass(tableIdentifier, columnDescriptor))
                                                .collect(Collectors.toUnmodifiableSet()),
                                        columnDescriptors
                                );
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toUnmodifiableSet()))
        ).build().writeToPath(Path.of(build_dir));
    }

    public static Connection getConnection() throws SQLException {
        Injector injector = Guice.createInjector(new DatabaseModule());
        DataSource dataSource = injector.getInstance(DataSource.class);
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        return connection;
    }

    private static Map<String, List<String>> getSelectedTables(String tablesJson) {
        return new Gson().fromJson(tablesJson, new TypeToken<Map<String, List<String>>>() {
        }.getType());
    }

    public static void main(String[] args) throws SQLException, IOException {
        try (Connection connection = getConnection()) {
            Exporter exporter = new Exporter(
                    connection,
                    args[1],
                    args[2]
            );
            exporter.export();

            Map<String, List<String>> tables = getSelectedTables(args[3]);
            for (String schemaIdentifier : exporter.getSchemaIdentifiers(tables.keySet())) {
                exporter.extractSchema(schemaIdentifier, tables);
            }
        }
    }
}
