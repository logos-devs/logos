package dev.logos.service.storage.pg.exporter;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.squareup.javapoet.*;
import dev.logos.app.register.registerModule;
import dev.logos.service.storage.EntityStorage;
import dev.logos.service.storage.EntityStorageService;
import dev.logos.service.storage.TableStorage;
import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.pg.*;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;
import dev.logos.service.storage.pg.exporter.module.annotation.BuildDir;
import dev.logos.service.storage.pg.exporter.module.annotation.BuildPackage;
import dev.logos.service.storage.validator.Validator;
import io.grpc.stub.StreamObserver;
import org.jdbi.v3.core.statement.Query;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static dev.logos.service.storage.pg.Identifier.snakeToCamelCase;
import static javax.lang.model.element.Modifier.*;

public class CodeGenerator {
    private final String buildDir;
    private final String buildPackage;
    private final Map<String, PgTypeMapper> pgColumnTypeMappers;

    @Inject
    public CodeGenerator(
            @BuildDir String buildDir,
            @BuildPackage String buildPackage,
            Map<String, PgTypeMapper> pgColumnTypeMappers
    ) {
        this.buildDir = buildDir;
        this.buildPackage = buildPackage;
        this.pgColumnTypeMappers = pgColumnTypeMappers;
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
                ClassName.get(buildPackage + "." + schemaDescriptor.name(),
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
                                .addException(EntityReadException.class)
                                .addParameter(ResultSet.class, "resultSet")
                                .returns(resultProtoClassName)
                                .addStatement(
                                        CodeBlock.builder()
                                                 .beginControlFlow("try")
                                                 .add(
                                                         CodeBlock.builder()
                                                                  .add("$T.Builder builder = $T.newBuilder();\n",
                                                                       resultProtoClassName,
                                                                       resultProtoClassName)
                                                                  .add(columnDescriptors
                                                                               .stream()
                                                                               .map(columnDescriptor -> {
                                                                                   String columnName = columnDescriptor.name();
                                                                                   String setterName = snakeToCamelCase(columnDescriptor.name());
                                                                                   PgTypeMapper typeMapper = getPgTypeMapper(columnDescriptor.type());
                                                                                   return CodeBlock.of(
                                                                                           "if (resultSet.getObject($S) != null) { builder.$N($L); }",
                                                                                           columnName,
                                                                                           "%s%s%s".formatted(
                                                                                                   typeMapper.protoFieldRepeated() ? "addAll" : "set",
                                                                                                   setterName.substring(0, 1).toUpperCase(),
                                                                                                   setterName.substring(1)
                                                                                           ),
                                                                                           typeMapper.pgToProto(
                                                                                                   CodeBlock.of(
                                                                                                           "$L resultSet.$N($S)",
                                                                                                           typeMapper.resultSetFieldCast(),
                                                                                                           typeMapper.resultSetFieldGetter(),
                                                                                                           columnName)));
                                                                               }).collect(CodeBlock.joining("\n")))
                                                                  .add("return builder.build()")
                                                                  .build()).build()
                                )
                                .nextControlFlow("catch ($T e)", SQLException.class)
                                .addStatement("throw new $T(e)", EntityReadException.class)
                                .endControlFlow()
                                .build())
                .addTypes(columnClasses);

        // Relation.getColumns
        MethodSpec.Builder getColumnsMethodBuilder =
                MethodSpec.methodBuilder("getColumns")
                          .addModifiers(PUBLIC)
                          .returns(ParameterizedTypeName.get(ClassName.get(Map.class),
                                                             ClassName.get(String.class),
                                                             ClassName.get(Column.class)));

        CodeBlock.Builder mapOfBuilder = CodeBlock.builder().add("return $T.ofEntries(", Map.class);

        int columnIndex = 0;
        for (ColumnDescriptor columnDescriptor : columnDescriptors) {
            String className = snakeToCamelCase(columnDescriptor.name());
            if (className.equals(tableDescriptor.getClassName().simpleName())) {
                className = className + "_";
            }

            ClassName columnClassName = ClassName.bestGuess(className);
            tableClassBuilder.addField(
                    FieldSpec.builder(columnClassName, columnDescriptor.getInstanceVariableName(), PUBLIC, STATIC, FINAL)
                             .initializer("new $T()", columnClassName)
                             .build());

            if (columnIndex > 0) {
                mapOfBuilder.add(",");
            }

            mapOfBuilder.add("\n  $T.entry($S, (Column) $L)", Map.class, columnDescriptor.name(),
                             columnDescriptor.getInstanceVariableName());
            columnIndex++;
        }

        mapOfBuilder.add("\n);");

        getColumnsMethodBuilder.addCode(mapOfBuilder.build());

        tableClassBuilder.addMethod(getColumnsMethodBuilder.build());

        // Relation.bindFields
        MethodSpec.Builder bindFieldsMethod = MethodSpec.methodBuilder("bindFields")
                                                        .addModifiers(PUBLIC)
                                                        .addParameter(ParameterizedTypeName.get(ClassName.get(Map.class),
                                                                                                ClassName.get(String.class),
                                                                                                ClassName.get(Object.class)), "fields")
                                                        .addParameter(ClassName.get(Query.class), "query");

        for (ColumnDescriptor columnDescriptor : columnDescriptors) {
            String columnType = columnDescriptor.type();
            if (!pgColumnTypeMappers.containsKey(columnType)) {
                throw new RuntimeException("There is no PgTypeMapper bound for type: " + columnType);
            }

            PgTypeMapper typeMapper = pgColumnTypeMappers.get(columnType);
            String columnName = columnDescriptor.name();
            CodeBlock typeMapperCodeBlock = typeMapper.protoToPg("query", "fields", columnName);
            bindFieldsMethod.addCode("if (fields.containsKey($S)) { $L\n }", columnName, typeMapperCodeBlock);
        }

        tableClassBuilder.addMethod(bindFieldsMethod.build());

        return tableClassBuilder.build();
    }

    public TypeSpec makeColumnClass(TableDescriptor tableDescriptor,
                                    ColumnDescriptor columnDescriptor) {
        String columnIdentifier = columnDescriptor.name();

        String columnClassNameStr = Identifier.snakeToCamelCase(columnIdentifier);
        if (columnClassNameStr.equals(tableDescriptor.getClassName().simpleName())) {
            columnClassNameStr = columnClassNameStr + "_";
        }
        ClassName columnClassName = ClassName.bestGuess(columnClassNameStr);

        return TypeSpec.classBuilder(columnClassName)
                       .addModifiers(PUBLIC, STATIC)
                       .superclass(Column.class)
                       .addMethod(MethodSpec.constructorBuilder()
                                            .addStatement("super($S, $S, $S)",
                                                          columnIdentifier,
                                                          Identifier.quoteIdentifier(tableDescriptor.name()) + '.'
                                                                  + Identifier.quoteIdentifier(columnIdentifier),
                                                          columnDescriptor.type())
                                            .build())
                       .build();
    }

    private String protoHeader(SchemaDescriptor schemaDescriptor, TableDescriptor tableDescriptor) {
        String packageName = buildPackage + "." + schemaDescriptor.name();
        String imports = tableDescriptor
                .columns()
                .stream()
                .flatMap(columnDescriptor -> pgColumnTypeMappers.get(columnDescriptor.type()).protoImports().stream())
                .map(importPath -> importPath.replaceAll("\"", ""))
                .map("import \"%s\";\n"::formatted).collect(Collectors.joining());

        return """
                syntax = "proto3";
                
                option java_package = "%s";
                option java_multiple_files = true;
                
                %s
                """.formatted(packageName, imports);
    }

    private String protoGetRequestMessage(String entityName) {
        return """
                message Get%sRequest {
                    bytes id = 1;
                }
                """.formatted(entityName);
    }

    private String protoGetResponseMessage(String entityName) {
        return """
                message Get%sResponse {
                    %s entity = 1;
                }
                """.formatted(entityName, entityName);
    }

    private String protoListRequestMessage(String entityName) {
        return """
                message List%sRequest {
                    int64 limit = 1;
                    int64 offset = 2;
                }
                """.formatted(entityName);
    }

    private String protoListResponseMessage(String entityName) {
        return """
                message List%sResponse {
                    repeated %s results = 1;
                }
                """.formatted(entityName, entityName);
    }

    private String protoCreateRequestMessage(String entityName) {
        return """
                message Create%sRequest {
                    %s entity = 1;
                }
                """.formatted(entityName, entityName);
    }

    private String protoCreateResponseMessage(String entityName) {
        return """
                message Create%sResponse {
                    bytes id = 1;
                }
                """.formatted(entityName);
    }

    private String protoUpdateRequestMessage(String entityName) {
        return """
                message Update%sRequest {
                    bytes id = 1;
                    %s entity = 2;
                    bool sparse = 3;
                }
                """.formatted(entityName, entityName);
    }

    private String protoUpdateResponseMessage(String entityName) {
        return """
                message Update%sResponse {
                    bytes id = 1;
                }
                """.formatted(entityName);
    }

    private String protoDeleteRequestMessage(String entityName) {
        return """
                message Delete%sRequest {
                    bytes id = 1;
                }
                """.formatted(entityName);
    }

    private String protoDeleteResponseMessage(String entityName) {
        return """
                message Delete%sResponse {
                    bytes id = 1;
                }
                """.formatted(entityName);
    }

    PgTypeMapper getPgTypeMapper(String type) {
        if (!pgColumnTypeMappers.containsKey(type)) {
            throw new RuntimeException("There is no PgTypeMapper bound for type: " + type);
        }

        return pgColumnTypeMappers.get(type);
    }

    private String protoEntityMessage(String entityName, List<ColumnDescriptor> columnDescriptors) {
        String fields = String.join("\n    ",
                                    IntStream.range(0, columnDescriptors.size())
                                             .mapToObj(i -> {
                                                 ColumnDescriptor columnDescriptor = columnDescriptors.get(i);
                                                 PgTypeMapper typeMapper = getPgTypeMapper(columnDescriptor.type());

                                                 return "%s%s %s = %s;".formatted(
                                                         typeMapper.protoFieldRepeated() ? "repeated " : "",
                                                         typeMapper.protoFieldTypeKeyword(),
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
        return """
                service %1$sStorageService {
                    rpc Create(Create%1$sRequest) returns (Create%1$sResponse);
                    rpc Update(Update%1$sRequest) returns (Update%1$sResponse);
                    rpc Delete(Delete%1$sRequest) returns (Delete%1$sResponse);
                    rpc Get(Get%1$sRequest) returns (Get%1$sResponse);
                    rpc List(List%1$sRequest) returns (List%1$sResponse);
                }
                """.formatted(entityName);
    }

    public String makeProtoService(SchemaDescriptor schemaDescriptor, TableDescriptor tableDescriptor) {
        String tableSimpleName = tableDescriptor.getClassName().simpleName();

        return String.join("\n",
                           List.of(
                                   protoHeader(schemaDescriptor, tableDescriptor),
                                   protoCreateRequestMessage(tableSimpleName),
                                   protoCreateResponseMessage(tableSimpleName),
                                   protoGetRequestMessage(tableSimpleName),
                                   protoGetResponseMessage(tableSimpleName),
                                   protoUpdateRequestMessage(tableSimpleName),
                                   protoUpdateResponseMessage(tableSimpleName),
                                   protoDeleteRequestMessage(tableSimpleName),
                                   protoDeleteResponseMessage(tableSimpleName),
                                   protoListRequestMessage(tableSimpleName),
                                   protoListResponseMessage(tableSimpleName),
                                   protoEntityMessage(tableSimpleName, tableDescriptor.columns()),
                                   protoService(tableSimpleName)
                           ));
    }

    MethodSpec makeRpcHandler(ClassName requestMessage, ClassName responseMessage, String methodName) {
        return MethodSpec.methodBuilder(methodName)
                         .addAnnotation(Override.class)
                         .addModifiers(PUBLIC)
                         .addParameter(requestMessage, "request")
                         .addParameter(makeStreamObserverType(responseMessage), "responseObserver")
                         .addStatement("$T.super.$L(request, responseObserver)",
                                       EntityStorageService.class,
                                       methodName)
                         .build();
    }

    MethodSpec makePreSaveMethod(ClassName entityMessage) {
        return MethodSpec.methodBuilder("preSave")
                         .addModifiers(PUBLIC)
                         .addParameter(entityMessage, "entity")
                         .returns(entityMessage)
                         .addStatement("return $L", "entity")
                         .build();
    }

    private MethodSpec makeEntityGetter(ClassName entityMessage, ClassName getRequestMessage, ClassName createRequestMessage, ClassName updateRequestMessage) {
        TypeVariableName requestType = TypeVariableName.get("Request");
        return MethodSpec.methodBuilder("entity")
                         .addModifiers(PUBLIC)
                         .addTypeVariable(requestType)
                         .addParameter(requestType, "request")
                         .returns(entityMessage)
                         .beginControlFlow("if (request instanceof $T)", createRequestMessage)
                         .addStatement("return preSave((($T)request).getEntity())", createRequestMessage)
                         .endControlFlow()
                         .beginControlFlow("if (request instanceof $T)", updateRequestMessage)
                         .addStatement("return preSave((($T)request).getEntity())", updateRequestMessage)
                         .endControlFlow()
                         .addStatement("throw new $T($S)", RuntimeException.class, "Unexpected request type")
                         .build();
    }

    // <Request> StorageIdentifier id(Request request);
    private MethodSpec makeIdGetter(
            ClassName storageIdentifier,
            ClassName getRequestMessage,
            ClassName updateRequestMessage,
            ClassName deleteRequestMessage) {

        TypeVariableName requestType = TypeVariableName.get("Request");

        return MethodSpec.methodBuilder("id")
                         .addModifiers(PUBLIC)
                         .addTypeVariable(requestType)
                         .addParameter(requestType, "request")
                         .returns(storageIdentifier)
                         .beginControlFlow("if (request instanceof $T)", getRequestMessage)
                         .addStatement("$T getRequest = ($T) request", getRequestMessage, getRequestMessage)
                         .addStatement("return $T.bytestringToUuid(getRequest.getId())", Converter.class)
                         .endControlFlow()
                         .beginControlFlow("if (request instanceof $T)", updateRequestMessage)
                         .addStatement("$T updateRequest = ($T) request", updateRequestMessage, updateRequestMessage)
                         .addStatement("return $T.bytestringToUuid(updateRequest.getId())", Converter.class)
                         .endControlFlow()
                         .beginControlFlow("if (request instanceof $T)", deleteRequestMessage)
                         .addStatement("$T deleteRequest = ($T) request", deleteRequestMessage, deleteRequestMessage)
                         .addStatement("return $T.bytestringToUuid(deleteRequest.getId())", Converter.class)
                         .endControlFlow()
                         .addStatement("throw new $T($S)", RuntimeException.class, "Unexpected request type")
                         .build();
    }

    private MethodSpec makeValidateRequestMethod(ClassName requestMessage, boolean validateEntity) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("validate")
                                              .addModifiers(PROTECTED)
                                              .addParameter(requestMessage, "request")
                                              .addParameter(ClassName.get(Validator.class), "validator");
        if (validateEntity) {
            method.addStatement("validate(entity(request), validator);");
        }
        return method.build();
    }

    private MethodSpec makeValidateEntityMethod(ClassName requestMessage) {
        return MethodSpec.methodBuilder("validate")
                         .addModifiers(PROTECTED)
                         .addParameter(requestMessage, "request")
                         .addParameter(ClassName.get(Validator.class), "validator")
                         .build();
    }

    public void makeStorageServiceBaseClass(SchemaDescriptor schemaDescriptor, TableDescriptor tableDescriptor) throws IOException {
        String packageName = buildPackage + "." + schemaDescriptor.name();
        ClassName tableClassName = tableDescriptor.getClassName();
        ClassName entityClassName = ClassName.bestGuess(String.format("%s.%s", packageName, tableClassName));
        ClassName storageIdentifier = ClassName.get(UUID.class);

        ParameterizedTypeName entityStorageClass = ParameterizedTypeName.get(
                ClassName.get(EntityStorage.class),
                entityClassName,
                storageIdentifier
        );

        ClassName createRequestMessage = ClassName.bestGuess(
                String.format("%s.Create%sRequest", packageName, tableClassName));
        ClassName createResponseMessage = ClassName.bestGuess(
                String.format("%s.Create%sResponse", packageName, tableClassName));
        ClassName listRequestMessage = ClassName.bestGuess(
                String.format("%s.List%sRequest", packageName, tableClassName));
        ClassName listResponseMessage = ClassName.bestGuess(
                String.format("%s.List%sResponse", packageName, tableClassName));
        ClassName getRequestMessage = ClassName.bestGuess(
                String.format("%s.Get%sRequest", packageName, tableClassName));
        ClassName getResponseMessage = ClassName.bestGuess(
                String.format("%s.Get%sResponse", packageName, tableClassName));
        ClassName updateRequestMessage = ClassName.bestGuess(
                String.format("%s.Update%sRequest", packageName, tableClassName));
        ClassName updateResponseMessage = ClassName.bestGuess(
                String.format("%s.Update%sResponse", packageName, tableClassName));
        ClassName deleteRequestMessage = ClassName.bestGuess(
                String.format("%s.Delete%sRequest", packageName, tableClassName));
        ClassName deleteResponseMessage = ClassName.bestGuess(
                String.format("%s.Delete%sResponse", packageName, tableClassName));
        ClassName entityMessage = ClassName.bestGuess(String.format("%s.%s", packageName, tableClassName));

        TypeSpec.Builder storageServiceClassSpec =
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
                                getRequestMessage,
                                getResponseMessage,
                                listRequestMessage,
                                listResponseMessage,
                                createRequestMessage,
                                createResponseMessage,
                                updateRequestMessage,
                                updateResponseMessage,
                                deleteRequestMessage,
                                deleteResponseMessage,
                                entityMessage,
                                ClassName.get(UUID.class)
                        ))
                        .addMethod(MethodSpec.methodBuilder("getStorage")
                                             .addAnnotation(Override.class)
                                             .addModifiers(PUBLIC)
                                             .returns(entityStorageClass)
                                             .addStatement("return this.storage")
                                             .build())
                        .addMethod(makeRpcHandler(getRequestMessage, getResponseMessage, "get"))
                        .addMethod(makeRpcHandler(listRequestMessage, listResponseMessage, "list"))
                        .addMethod(makeRpcHandler(createRequestMessage, createResponseMessage, "create"))
                        .addMethod(makeRpcHandler(updateRequestMessage, updateResponseMessage, "update"))
                        .addMethod(makeRpcHandler(deleteRequestMessage, deleteResponseMessage, "delete"))
                        .addMethod(makePreSaveMethod(entityMessage))
                        .addMethod(makeEntityGetter(entityMessage, getRequestMessage, createRequestMessage, updateRequestMessage))
                        .addMethod(makeIdGetter(storageIdentifier, getRequestMessage, updateRequestMessage, deleteRequestMessage))

                        // GetResponse response(Stream<Entity>, GetRequest)
                        .addMethod(MethodSpec.methodBuilder("response")
                                             .addModifiers(PUBLIC)
                                             .returns(TypeVariableName.get("Response"))
                                             .addTypeVariable(TypeVariableName.get("Request"))
                                             .addTypeVariable(TypeVariableName.get("Response"))
                                             .addParameter(
                                                     ParameterizedTypeName.get(ClassName.get(Stream.class),
                                                                               entityMessage),
                                                     "%sStream".formatted(tableDescriptor.getInstanceVariableName()))
                                             .addParameter(TypeVariableName.get("Request"), "request")
                                             .addStatement("""
                                                                   return switch(request) {
                                                                       case $T r -> ($T)$T.newBuilder().setEntity($LStream.findFirst().get()).build();
                                                                       case $T r -> ($T)$T.newBuilder().addAllResults($LStream.toList()).build();
                                                                       default -> throw new $T("Unexpected request type");
                                                                   }
                                                                   """,
                                                           getRequestMessage,
                                                           TypeVariableName.get("Response"),
                                                           getResponseMessage,
                                                           tableDescriptor.getInstanceVariableName(),
                                                           listRequestMessage,
                                                           TypeVariableName.get("Response"),
                                                           listResponseMessage,
                                                           tableDescriptor.getInstanceVariableName(),
                                                           RuntimeException.class).build())
                        .addMethod(
                                MethodSpec.methodBuilder("response")
                                          .addAnnotation(Override.class)
                                          .addModifiers(PUBLIC)
                                          .addTypeVariable(TypeVariableName.get("Request"))
                                          .addTypeVariable(TypeVariableName.get("Response"))
                                          .returns(TypeVariableName.get("Response"))
                                          .addParameter(UUID.class, "id")
                                          .addParameter(TypeVariableName.get("Request"), "request")
                                          .beginControlFlow("if (request instanceof $T)", createRequestMessage)
                                          .addStatement("return (Response) $T.newBuilder().build()", createResponseMessage)
                                          .endControlFlow()
                                          .addStatement("return null")
                                          .build());

        ;

        storageServiceClassSpec.addMethod(makeValidateRequestMethod(listRequestMessage, false))
                               .addMethod(makeValidateRequestMethod(createRequestMessage, true))
                               .addMethod(makeValidateRequestMethod(updateRequestMessage, true))
                               .addMethod(makeValidateRequestMethod(deleteRequestMessage, false));

        // entity-level validator that applies to creates and updates
        storageServiceClassSpec.addMethod(makeValidateEntityMethod(entityMessage));

        JavaFile.builder(packageName, storageServiceClassSpec.build())
                .addStaticImport(
                        ClassName.bestGuess(String.format("%s.%s", buildPackage, schemaDescriptor.getClassName())),
                        tableDescriptor.getInstanceVariableName())
                .build()
                .writeToPath(Path.of(buildDir));
    }

    private static ParameterizedTypeName makeStreamObserverType(ClassName responseMessage) {
        return ParameterizedTypeName.get(ClassName.get(StreamObserver.class), responseMessage);
    }

    public void makeStorageModule(
            SchemaDescriptor schemaDescriptor,
            TableDescriptor tableDescriptor
    ) throws IOException {
        String packageName = buildPackage + "." + schemaDescriptor.name() + "." + tableDescriptor.name();

        ClassName entityClassName = ClassName.bestGuess(
                buildPackage + "." + schemaDescriptor.name() + "." + tableDescriptor.getClassName());
        String tableInstanceVariableName = tableDescriptor.getInstanceVariableName();

        JavaFile.builder(packageName,
                         TypeSpec.classBuilder("StorageModule")
                                 .addAnnotation(AnnotationSpec.builder(registerModule.class).build())
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
                                                                              entityClassName,
                                                                              ClassName.get(UUID.class)
                                                                      )
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
                        ClassName.bestGuess(String.format("%s.%s", buildPackage, schemaDescriptor.getClassName())),
                        tableInstanceVariableName)
                .build()
                .writeToPath(Path.of(buildDir));
    }
}
