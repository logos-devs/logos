package dev.logos.stack.service.storage.exporter;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.protobuf.DescriptorProtos;
import com.squareup.javapoet.*;
import dev.logos.stack.service.storage.EntityStorage;
import dev.logos.stack.service.storage.EntityStorageService;
import dev.logos.stack.service.storage.TableStorage;
import dev.logos.stack.service.storage.pg.Column;
import dev.logos.stack.service.storage.pg.Identifier;
import dev.logos.stack.service.storage.pg.Relation;
import dev.logos.stack.service.storage.pg.Schema;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static dev.logos.stack.service.storage.pg.Identifier.snakeToCamelCase;
import static javax.lang.model.element.Modifier.*;

public class CodeGenerator {
    private final String build_dir;
    private final String build_package;

    public CodeGenerator(String build_dir, String build_package) {
        this.build_dir = build_dir;
        this.build_package = build_package;
    }

    private static final String[] JAVA_KEYWORDS = {"abstract", "continue", "for", "new", "switch", "assert", "default",
            "goto", "package", "synchronized", "boolean", "do", "if", "private", "this", "break", "double",
            "implements", "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum",
            "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final",
            "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile", "const", "float",
            "native", "super", "while"};

    public static String classNameToInstanceName(String className) {
        String instanceName = className.substring(0, 1).toLowerCase() + className.substring(1);
        if (Arrays.asList(JAVA_KEYWORDS).contains(instanceName)) {
            instanceName = "_" + instanceName;
        }
        return instanceName;
    }

    public TypeSpec makeSchemaClass(SchemaDescriptor schemaDescriptor,
                                    Iterable<TypeSpec> tableClasses) {

        TypeSpec.Builder schemaClassBuilder = TypeSpec.classBuilder(schemaDescriptor.getClassName())
                                                      .addModifiers(PUBLIC)
                                                      .superclass(Schema.class)
                                                      .addMethod(MethodSpec.constructorBuilder()
                                                                           .addStatement("super($S, $S)",
                                                                                         schemaDescriptor.name(),
                                                                                         Identifier.quoteIdentifier(
                                                                                                 schemaDescriptor.name()))
                                                                           .build())
                                                      .addTypes(tableClasses);

        for (TypeSpec tableClass : tableClasses) {
            ClassName tableClassName = ClassName.bestGuess(tableClass.name);
            schemaClassBuilder.addField(
                    FieldSpec.builder(tableClassName,
                                      classNameToInstanceName(tableClass.name),
                                      PUBLIC, STATIC, FINAL)
                             .initializer("new $T()", tableClassName)
                             .build());
        }

        return schemaClassBuilder.build();
    }

    public TypeSpec makeTableClass(SchemaDescriptor schemaDescriptor,
                                   TableDescriptor tableDescriptor,
                                   Iterable<TypeSpec> columnClasses,
                                   List<ColumnDescriptor> columnDescriptors) {

        ClassName resultProtoClassName =
                ClassName.get(build_package + "." + schemaDescriptor.name(),
                              tableDescriptor.getClassName().simpleName());

        TypeSpec.Builder tableClassBuilder = TypeSpec
                .classBuilder(tableDescriptor.getClassName())
                .addModifiers(PUBLIC, STATIC)
                .superclass(Relation.class)
                .addMethod(MethodSpec.constructorBuilder()
                                     .addStatement("super($S, $S)",
                                                   tableDescriptor.name(),
                                                   Identifier.quoteIdentifier(
                                                           schemaDescriptor.name()) + "." + Identifier.quoteIdentifier(
                                                           tableDescriptor.name()))
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
                                                 .add("$T.Builder builder = $T.newBuilder();\n", resultProtoClassName,
                                                      resultProtoClassName)
                                                 .add(columnDescriptors
                                                              .stream()
                                                              .map(columnDescriptor -> {
                                                                  String columnName = columnDescriptor.name();
                                                                  return CodeBlock.of(
                                                                          "if (resultSet.getObject($S) != null) { builder.$N($L); }\n",
                                                                          columnName,
                                                                          columnDescriptor.getProtobufFieldSetter(),
                                                                          columnDescriptor.convertType(
                                                                                  CodeBlock.of(
                                                                                          "%sresultSet.$N($S)".formatted(
                                                                                                  columnDescriptor.getJavaCast()),
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
                                      PUBLIC, STATIC, FINAL)
                             .initializer("new $T()", columnClassName)
                             .build());
        }

        return tableClassBuilder.build();
    }


    public TypeSpec makeColumnClass(TableDescriptor tableDescriptor,
                                    ColumnDescriptor columnDescriptor) {
        String columnIdentifier = columnDescriptor.name();
        String columnType = columnDescriptor.type();

        String columnClassNameStr = snakeToCamelCase(columnIdentifier);
        if (columnClassNameStr.equals(tableDescriptor.getClassName().simpleName())) {
            columnClassNameStr = columnClassNameStr + "_";
        }
        ClassName columnClassName = ClassName.bestGuess(columnClassNameStr);

        return TypeSpec.classBuilder(columnClassName)
                       .addModifiers(PUBLIC, STATIC)
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
                                                          Identifier.quoteIdentifier(tableDescriptor.name()) + '.'
                                                          + Identifier.quoteIdentifier(columnIdentifier),
                                                          columnDescriptor.type())
                                            .build())
                       .build();
    }

    private String protoHeader(String packageName) {
        // language=proto
        return """
                syntax = "proto3";

                option java_package = "%s";
                option java_multiple_files = true;
                """.formatted(packageName);
    }

    private String protoRequestMessage(String entityName) {
        // language=proto
        return """
                message List%sRequest {
                    int64 limit = 1;
                    int64 offset = 2;
                }
                    """.formatted(entityName);
    }

    private String protoResponseMessage(String entityName) {
        // language=proto
        return """
                message List%sResponse {
                    repeated %s results = 1;
                }
                """.formatted(entityName, entityName);
    }

    private String protoEntityMessage(String entityName, List<ColumnDescriptor> columnDescriptors) {
        // language=proto
        String fields = String.join("\n    ",
                                    IntStream.range(0, columnDescriptors.size())
                                             .mapToObj(i -> {
                                                 ColumnDescriptor columnDescriptor = columnDescriptors.get(i);

                                                 return "%s%s %s = %s;".formatted(
                                                         columnDescriptor.isArray() ? "repeated " : "",
                                                         columnDescriptor.getProtobufTypeName(),
                                                         columnDescriptor.name(),
                                                         i + 1
                                                 );
                                             })
                                             .toList());
        return """
                message %s {
                    %s
                }
                """.formatted(entityName, fields);
    }

    private String protoService(String entityName) {
        // language=proto
        return """
                service %sStorageService {
                    rpc List(List%sRequest) returns (List%sResponse);
                }
                """.formatted(entityName, entityName, entityName);
    }

    public String makeProtoService(SchemaDescriptor schemaDescriptor, TableDescriptor tableDescriptor) {
        String tableSimpleName = tableDescriptor.getClassName().simpleName();

        return String.join("\n",
                           List.of(
                                   protoHeader(build_package + "." + schemaDescriptor.name()),
                                   protoRequestMessage(tableSimpleName),
                                   protoResponseMessage(tableSimpleName),
                                   protoEntityMessage(tableSimpleName, tableDescriptor.columns()),
                                   protoService(tableSimpleName)
                           ));
    }

    public DescriptorProtos.FileDescriptorProto makeResultProtoFileDescriptor(SchemaDescriptor schemaDescriptor, TableDescriptor tableDescriptor) {

        ClassName tableClassName = tableDescriptor.getClassName();
        String tableSimpleName = tableClassName.simpleName();

        return DescriptorProtos.FileDescriptorProto
                .newBuilder()
                .setName(build_package.replace(".", "/") + "/" + schemaDescriptor.name() + "/" + tableDescriptor.name() + ".proto")
                .setSyntax("proto3")
                .setOptions(
                        DescriptorProtos.FileOptions
                                .newBuilder()
                                .setJavaPackage(build_package + "." + schemaDescriptor.name())
                                .setJavaMultipleFiles(true))
                .addMessageType(
                        DescriptorProtos.DescriptorProto
                                .newBuilder()
                                .setName("List" + tableSimpleName + "Request")
                                .addField(
                                        DescriptorProtos.FieldDescriptorProto
                                                .newBuilder()
                                                .setName("limit")
                                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64)
                                                .setNumber(1))
                                .addField(
                                        DescriptorProtos.FieldDescriptorProto
                                                .newBuilder()
                                                .setName("offset")
                                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64)
                                                .setNumber(2)))
                .addMessageType(
                        DescriptorProtos.DescriptorProto
                                .newBuilder()
                                .setName("List" + tableSimpleName + "Response")
                                .addField(
                                        DescriptorProtos.FieldDescriptorProto
                                                .newBuilder()
                                                .setName("results")
                                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
                                                .setTypeName(tableSimpleName)
                                                .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED)
                                                .setNumber(1)))
                .addMessageType(
                        DescriptorProtos.DescriptorProto
                                .newBuilder()
                                .setName(tableSimpleName)
                                .addAllField(
                                        IntStream.range(0, tableDescriptor.columns().size())
                                                 .mapToObj(i -> {
                                                     ColumnDescriptor columnDescriptor = tableDescriptor.columns()
                                                                                                        .get(i);
                                                     DescriptorProtos.FieldDescriptorProto.Builder fieldDescriptorProto = DescriptorProtos.FieldDescriptorProto
                                                             .newBuilder()
                                                             .setName(columnDescriptor.name())
                                                             .setType(
                                                                     columnDescriptor.getProtobufType())
                                                             .setNumber(i + 1);

                                                     if (columnDescriptor.isArray()) {
                                                         fieldDescriptorProto.setLabel(
                                                                 DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED);
                                                     }

                                                     return fieldDescriptorProto.build();
                                                 }).toList()))
                .addService(
                        DescriptorProtos.ServiceDescriptorProto
                                .newBuilder()
                                .setName(tableSimpleName + "StorageService")
                                .addMethod(
                                        DescriptorProtos.MethodDescriptorProto
                                                .newBuilder()
                                                .setName("List")
                                                .setInputType("List" + tableSimpleName + "Request")
                                                .setOutputType(
                                                        "List" + tableSimpleName + "Response")))
                .build();
    }

    public void makeStorageServiceBaseClass(SchemaDescriptor schemaDescriptor, TableDescriptor tableDescriptor) throws IOException {
        String packageName = build_package + "." + schemaDescriptor.name();
        ClassName tableClassName = tableDescriptor.getClassName();
        ClassName entityClassName = ClassName.bestGuess(String.format("%s.%s", packageName, tableClassName));
        ParameterizedTypeName entityStorageClass = ParameterizedTypeName.get(
                ClassName.get(EntityStorage.class),
                entityClassName
        );

        JavaFile.builder(packageName,
                         TypeSpec.classBuilder(String.format("%sStorageServiceBase", tableClassName))
                                 .addModifiers(PUBLIC, ABSTRACT)
                                 .superclass(ClassName.bestGuess(
                                         String.format("%s.%sStorageServiceGrpc.%sStorageServiceImplBase", packageName,
                                                       tableClassName, tableClassName)))
                                 .addField(FieldSpec.builder(entityStorageClass, "storage", PRIVATE)
                                                    .addAnnotation(Inject.class)
                                                    .build())
                                 .addSuperinterface(ParameterizedTypeName.get(
                                         ClassName.get(EntityStorageService.class),
                                         ClassName.bestGuess(
                                                 String.format("%s.List%sRequest", packageName, tableClassName)),
                                         ClassName.bestGuess(
                                                 String.format("%s.List%sResponse", packageName, tableClassName)),
                                         ClassName.bestGuess(String.format("%s.%s", packageName, tableClassName)) // ,
                                 ))
                                 .addMethod(MethodSpec.methodBuilder("getStorage")
                                                      .addAnnotation(Override.class)
                                                      .addModifiers(PUBLIC)
                                                      .returns(entityStorageClass)
                                                      .addStatement("return this.storage")
                                                      .build())
                                 .addMethod(MethodSpec.methodBuilder("list")
                                                      .addAnnotation(Override.class)
                                                      .addModifiers(PUBLIC)
                                                      .addParameter(ClassName.bestGuess(
                                                                            String.format("%s.List%sRequest", packageName,
                                                                                          tableClassName)),
                                                                    "request")
                                                      .addParameter(ParameterizedTypeName.get(ClassName.get(
                                                                                                      StreamObserver.class),
                                                                                              ClassName.bestGuess(
                                                                                                      String.format(
                                                                                                              "%s.List%sResponse",
                                                                                                              packageName,
                                                                                                              tableClassName))),
                                                                    "responseObserver")
                                                      .addStatement("listHandler(request, responseObserver)")
                                                      .build())
                                 .addMethod(MethodSpec.methodBuilder("result")
                                                      .addAnnotation(Override.class)
                                                      .addModifiers(PUBLIC)
                                                      .addParameter(
                                                              ParameterizedTypeName.get(ClassName.get(Stream.class),
                                                                                        ClassName.bestGuess(
                                                                                                String.format("%s.%s",
                                                                                                              packageName,
                                                                                                              tableClassName))),
                                                              String.format("%sListStream",
                                                                            tableDescriptor.getInstanceVariableName()))
                                                      .returns(ClassName.bestGuess(
                                                              String.format("%s.List%sResponse", packageName,
                                                                            tableClassName)))
                                                      .addStatement(
                                                              String.format(
                                                                      "return $T.newBuilder().addAllResults(%sListStream.toList()).build()",
                                                                      tableDescriptor.getInstanceVariableName()),
                                                              ClassName.bestGuess(
                                                                      String.format("%s.List%sResponse", packageName,
                                                                                    tableClassName)))
                                                      .build())
                                 .build()
                )
                .addStaticImport(
                        ClassName.bestGuess(String.format("%s.%s", build_package, schemaDescriptor.getClassName())),
                        tableDescriptor.getInstanceVariableName())
                .build()
                .writeToPath(Path.of(build_dir));
    }

    public void makeStorageModule(
            SchemaDescriptor schemaDescriptor,
            TableDescriptor tableDescriptor
    ) throws IOException {
        String packageName = build_package + "." + schemaDescriptor.name() + "." + tableDescriptor.name();

        ClassName entityClassName = ClassName.bestGuess(
                build_package + "." + schemaDescriptor.name() + "." + tableDescriptor.getClassName());
        String tableInstanceVariableName = tableDescriptor.getInstanceVariableName();

        JavaFile.builder(packageName,
                         TypeSpec.classBuilder("StorageModule")
                                 .addModifiers(PUBLIC)
                                 .superclass(AbstractModule.class)
                                 .addMethod(MethodSpec.methodBuilder("configure")
                                                      .addAnnotation(Override.class)
                                                      .addModifiers(PROTECTED)
                                                      .addStatement(
                                                              String.format(
                                                                      "bind(new $T(){}).toInstance(new $T(%s, $T.class, $T.class))",
                                                                      tableInstanceVariableName),
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
                .addStaticImport(
                        ClassName.bestGuess(String.format("%s.%s", build_package, schemaDescriptor.getClassName())),
                        tableInstanceVariableName)
                .build()
                .writeToPath(Path.of(build_dir));
    }

}
