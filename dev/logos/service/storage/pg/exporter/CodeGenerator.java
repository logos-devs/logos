package dev.logos.service.storage.pg.exporter;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.GeneratedMessageV3;
import com.squareup.javapoet.*;
import dev.logos.app.register.registerModule;
import dev.logos.service.storage.EntityStorage;
import dev.logos.service.storage.EntityStorageService;
import dev.logos.service.storage.TableStorage;
import dev.logos.service.storage.pg.*;
import dev.logos.service.storage.validator.Validator;
import dev.logos.user.User;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

        MethodSpec.Builder getColumnsMethodBuilder =
            MethodSpec.methodBuilder("getColumns")
                      .addModifiers(PUBLIC)
                      .returns(
                          ParameterizedTypeName.get(ClassName.get(Map.class),
                                                    ClassName.get(String.class),
                                                    ClassName.get(Column.class)));

        CodeBlock.Builder mapOfBuilder = CodeBlock.builder().add("return $T.ofEntries(", Map.class);

        int columnIndex = 0;
        for (ColumnDescriptor columnDescriptor : columnDescriptors) {
            ClassName columnClassName = columnDescriptor.getClassName(tableDescriptor.getClassName().simpleName());
            tableClassBuilder.addField(
                FieldSpec.builder(columnClassName, columnDescriptor.getInstanceVariableName(), PUBLIC, STATIC, FINAL)
                         .initializer("new $T()", columnClassName)
                         .build());

            if (columnIndex > 0) {
                mapOfBuilder.add(",");
            }
            mapOfBuilder.add("$T.entry($S, (Column)$L)", Map.class, columnDescriptor.name(),
                             columnDescriptor.getInstanceVariableName());
            columnIndex++;
        }

        mapOfBuilder.add(");");

        getColumnsMethodBuilder.addCode(mapOfBuilder.build());

        tableClassBuilder.addMethod(getColumnsMethodBuilder.build());

        return tableClassBuilder.build();
    }

    public TypeSpec makeColumnClass(TableDescriptor tableDescriptor,
                                    ColumnDescriptor columnDescriptor) {
        String columnIdentifier = columnDescriptor.name();
        String columnType = columnDescriptor.type();

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

    private String protoHeader(String packageName) {
        // language=proto
        return """
            syntax = "proto3";

            option java_package = "%s";
            option java_multiple_files = true;
            """.formatted(packageName);
    }

    private String protoListRequestMessage(String entityName) {
        // language=proto
        return """
            message List%sRequest {
                int64 limit = 1;
                int64 offset = 2;
            }
                """.formatted(entityName);
    }

    private String protoListResponseMessage(String entityName) {
        // language=proto
        return """
            message List%sResponse {
                repeated %s results = 1;
            }
            """.formatted(entityName, entityName);
    }

    private String protoCreateRequestMessage(String entityName) {
        // language=proto
        return """
            message Create%sRequest {
                %s entity = 1;
            }
                """.formatted(entityName, entityName);
    }

    private String protoCreateResponseMessage(String entityName) {
        // language=proto
        return """
            message Create%sResponse {
                bytes id = 1;
            }
            """.formatted(entityName);
    }

    private String protoUpdateRequestMessage(String entityName) {
        // language=proto
        return """
            message Update%sRequest {
                bytes id = 1;
                %s entity = 2;
                bool sparse = 3;
            }
                """.formatted(entityName, entityName);
    }

    private String protoUpdateResponseMessage(String entityName) {
        // language=proto
        return """
            message Update%sResponse {
                bytes id = 1;
            }
            """.formatted(entityName);
    }

    private String protoDeleteRequestMessage(String entityName) {
        // language=proto
        return """
            message Delete%sRequest {
                bytes id = 1;
            }
                """.formatted(entityName);
    }

    private String protoDeleteResponseMessage(String entityName) {
        // language=proto
        return """
            message Delete%sResponse {
                bytes id = 1;
            }
            """.formatted(entityName);
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
                rpc Create(Create%sRequest) returns (Create%sResponse);
                rpc Update(Update%sRequest) returns (Update%sResponse);
                rpc Delete(Delete%sRequest) returns (Delete%sResponse);
                rpc List(List%sRequest) returns (List%sResponse);
            }
            """.formatted(entityName, entityName, entityName, entityName,
                          entityName, entityName, entityName, entityName,
                          entityName);
    }

    public String makeProtoService(SchemaDescriptor schemaDescriptor, TableDescriptor tableDescriptor) {
        String tableSimpleName = tableDescriptor.getClassName().simpleName();

        return String.join("\n",
                           List.of(
                               protoHeader(build_package + "." + schemaDescriptor.name()),
                               protoCreateRequestMessage(tableSimpleName),
                               protoCreateResponseMessage(tableSimpleName),
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

    public DescriptorProtos.FileDescriptorProto makeResultProtoFileDescriptor(SchemaDescriptor schemaDescriptor, TableDescriptor tableDescriptor) {

        ClassName tableClassName = tableDescriptor.getClassName();
        String tableSimpleName = tableClassName.simpleName();

        FieldDescriptorProto identifierField = FieldDescriptorProto
            .newBuilder()
            .setName("id")
            .setType(FieldDescriptorProto.Type.TYPE_BYTES)
            .setNumber(1)
            .build();

        FieldDescriptorProto entityField =
            FieldDescriptorProto
                .newBuilder()
                .setName("entity")
                .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                .setTypeName(tableSimpleName)
                .setNumber(2)
                .build();

        return DescriptorProtos.FileDescriptorProto
            .newBuilder()
            .setName(build_package.replace(".",
                                           "/") + "/" + schemaDescriptor.name() + "/" + tableDescriptor.name() + ".proto")
            .setSyntax("proto3")
            .setOptions(
                DescriptorProtos.FileOptions
                    .newBuilder()
                    .setJavaPackage(build_package + "." + schemaDescriptor.name())
                    .setJavaMultipleFiles(true))
            .addMessageType(
                DescriptorProtos.DescriptorProto
                    .newBuilder()
                    .setName("Create" + tableSimpleName + "Request")
                    .addField(identifierField)
                    .addField(entityField))
            .addMessageType(
                DescriptorProtos.DescriptorProto
                    .newBuilder()
                    .setName("Create" + tableSimpleName + "Response")
                    .addField(identifierField))
            .addMessageType(
                DescriptorProtos.DescriptorProto
                    .newBuilder()
                    .setName("Update" + tableSimpleName + "Request")
                    .addField(identifierField)
                    .addField(entityField))
            .addMessageType(
                DescriptorProtos.DescriptorProto
                    .newBuilder()
                    .setName("Update" + tableSimpleName + "Response")
                    .addField(identifierField))
            .addMessageType(
                DescriptorProtos.DescriptorProto
                    .newBuilder()
                    .setName("Delete" + tableSimpleName + "Request")
                    .addField(identifierField))
            .addMessageType(
                DescriptorProtos.DescriptorProto
                    .newBuilder()
                    .setName("Delete" + tableSimpleName + "Response")
                    .addField(identifierField))
            .addMessageType(
                DescriptorProtos.DescriptorProto
                    .newBuilder()
                    .setName("List" + tableSimpleName + "Request")
                    .addField(
                        FieldDescriptorProto
                            .newBuilder()
                            .setName("limit")
                            .setType(FieldDescriptorProto.Type.TYPE_INT64)
                            .setNumber(1))
                    .addField(
                        FieldDescriptorProto
                            .newBuilder()
                            .setName("offset")
                            .setType(FieldDescriptorProto.Type.TYPE_INT64)
                            .setNumber(2)))
            .addMessageType(
                DescriptorProtos.DescriptorProto
                    .newBuilder()
                    .setName("List" + tableSimpleName + "Response")
                    .addField(
                        FieldDescriptorProto
                            .newBuilder()
                            .setName("results")
                            .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                            .setTypeName(tableSimpleName)
                            .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
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
                                     FieldDescriptorProto.Builder fieldDescriptorProto = FieldDescriptorProto
                                         .newBuilder()
                                         .setName(columnDescriptor.name())
                                         .setType(
                                             columnDescriptor.getProtobufType())
                                         .setNumber(i + 1);

                                     if (columnDescriptor.isArray()) {
                                         fieldDescriptorProto = fieldDescriptorProto.setLabel(
                                             FieldDescriptorProto.Label.LABEL_REPEATED);
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
                            .setOutputType("List" + tableSimpleName + "Response"))
                    .addMethod(
                        DescriptorProtos.MethodDescriptorProto
                            .newBuilder()
                            .setName("Create")
                            .setInputType("Create" + tableSimpleName + "Request")
                            .setOutputType("Create" + tableSimpleName + "Response"))
                    .addMethod(
                        DescriptorProtos.MethodDescriptorProto
                            .newBuilder()
                            .setName("Update")
                            .setInputType("Update" + tableSimpleName + "Request")
                            .setOutputType("Update" + tableSimpleName + "Response"))
                    .addMethod(
                        DescriptorProtos.MethodDescriptorProto
                            .newBuilder()
                            .setName("Delete")
                            .setInputType("Delete" + tableSimpleName + "Request")
                            .setOutputType("Delete" + tableSimpleName + "Response"))
            ).build();
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

    private MethodSpec makeEntityGetter(String methodName, ClassName entityMessage, ClassName createRequestMessage, ClassName updateRequestMessage) {
        TypeVariableName requestType = TypeVariableName.get("Request");
        return MethodSpec.methodBuilder(methodName)
                         .addModifiers(PUBLIC)
                         .addTypeVariable(requestType)
                         .addParameter(requestType, "request")
                         .returns(entityMessage)
                         .beginControlFlow("if (request instanceof $T)", createRequestMessage)
                         .addStatement("return (($T)request).getEntity()", createRequestMessage)
                         .endControlFlow()
                         .beginControlFlow("if (request instanceof $T)", updateRequestMessage)
                         .addStatement("return (($T)request).getEntity()", updateRequestMessage)
                         .endControlFlow()
                         .addStatement("throw new $T($S)", RuntimeException.class, "Unexpected request type")
                         .build();
    }

    // <Request> StorageIdentifier id(Request request);
    private MethodSpec makeIdGetter(
        String methodName,
        ClassName storageIdentifier,
        ClassName updateRequestMessage,
        ClassName deleteRequestMessage) {

        TypeVariableName requestType = TypeVariableName.get("Request");

        return MethodSpec.methodBuilder(methodName)
                         .addModifiers(PUBLIC)
                         .addTypeVariable(requestType)
                         .addParameter(requestType, "request")
                         .returns(storageIdentifier)
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

    private MethodSpec makeValidateMethod(ClassName requestMessage) {
        return MethodSpec.methodBuilder("validate")
                         .addModifiers(PROTECTED)
                         .addParameter(requestMessage, "request")
                         .addParameter(ClassName.get(Validator.class), "validator")
                         .build();
    }

    private static MethodSpec makeAllowMethod(ClassName requestMessage) {
        return MethodSpec.methodBuilder("allow")
                         .addModifiers(PROTECTED)
                         .addParameter(requestMessage, "request")
                         .addParameter(ClassName.get(User.class), "user")
                         .returns(boolean.class)
                         .addStatement("return false")
                         .build();
    }

    public void makeStorageServiceBaseClass(SchemaDescriptor schemaDescriptor, TableDescriptor tableDescriptor) throws IOException {
        String packageName = build_package + "." + schemaDescriptor.name();
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
                    .addMethod(makeRpcHandler(listRequestMessage, listResponseMessage, "list"))
                    .addMethod(makeRpcHandler(createRequestMessage, createResponseMessage, "create"))
                    .addMethod(makeRpcHandler(updateRequestMessage, updateResponseMessage, "update"))
                    .addMethod(makeRpcHandler(deleteRequestMessage, deleteResponseMessage, "delete"))
                    .addMethod(makeEntityGetter("entity", entityMessage, createRequestMessage, updateRequestMessage))
                    .addMethod(makeIdGetter("id", storageIdentifier, updateRequestMessage, deleteRequestMessage))

                    // ListResponse response(Stream<Entity>, ListRequest)
                    .addMethod(MethodSpec.methodBuilder("response")
                                         .addModifiers(PUBLIC)
                                         .addParameter(
                                             ParameterizedTypeName.get(ClassName.get(Stream.class),
                                                                       entityMessage),
                                             String.format("%sListStream",
                                                           tableDescriptor.getInstanceVariableName()))
                                         .addParameter(listRequestMessage, "request")
                                         .returns(listResponseMessage)
                                         .addStatement(
                                             "return $T.newBuilder().addAllResults($LListStream.toList()).build()",
                                             listResponseMessage,
                                             tableDescriptor.getInstanceVariableName())
                                         .build())

                    // <Req extends GeneratedMessageV4, Resp extends GeneratedMessageV3> Resp response(StorageIdentifier, Req)
                    .addMethod(
                        MethodSpec.methodBuilder("response")
                                  .addAnnotation(Override.class)
                                  .addModifiers(PUBLIC)
                                  .addTypeVariable(TypeVariableName.get("Req", GeneratedMessageV3.class))
                                  .addTypeVariable(TypeVariableName.get("Resp", GeneratedMessageV3.class))
                                  .returns(TypeVariableName.get("Resp"))
                                  .addParameter(UUID.class, "id")
                                  .addParameter(TypeVariableName.get("Req"), "request")
                                  .beginControlFlow("if (request instanceof $T)", createRequestMessage)
                                  .addStatement("return (Resp) $T.newBuilder().build()", createResponseMessage)
                                  .endControlFlow()
                                  .addStatement("return null")
                                  .build()

                    )
//                    .addMethod(MethodSpec.methodBuilder("response")
//                                         .addModifiers(PUBLIC)
//                                         .addParameter(ClassName.get(UUID.class), "id")
//                                         .addParameter(createRequestMessage, "request")
//                                         .returns(createResponseMessage)
//                                         .addStatement("return $T.newBuilder().setId($T.uuidToBytestring(id)).build()",
//                                                       createResponseMessage,
//                                                       Converter.class)
//                                         .build())
//                    .addMethod(MethodSpec.methodBuilder("response")
//                                         .addModifiers(PUBLIC)
//                                         .addParameter(ClassName.get(UUID.class), "id")
//                                         .addParameter(updateRequestMessage, "request")
//                                         .returns(updateResponseMessage)
//                                         .addStatement("return $T.newBuilder().setId($T.uuidToBytestring(id)).build()",
//                                                       updateResponseMessage,
//                                                       Converter.class)
//                                         .build())
//                    .addMethod(MethodSpec.methodBuilder("response")
//                                         .addModifiers(PUBLIC)
//                                         .addParameter(ClassName.get(UUID.class), "id")
//                                         .addParameter(deleteRequestMessage, "request")
//                                         .returns(deleteResponseMessage)
//                                         .addStatement("return $T.newBuilder().setId($T.uuidToBytestring(id)).build()",
//                                                       deleteResponseMessage,
//                                                       Converter.class)
//                                         .build())
            ;

        for (ClassName message : List.of(listRequestMessage, createRequestMessage, updateRequestMessage,
                                         deleteRequestMessage)) {
            storageServiceClassSpec
                .addMethod(makeAllowMethod(message))
                .addMethod(makeValidateMethod(message));
        }

        JavaFile.builder(packageName, storageServiceClassSpec.build())
                .addStaticImport(
                    ClassName.bestGuess(String.format("%s.%s", build_package, schemaDescriptor.getClassName())),
                    tableDescriptor.getInstanceVariableName())
                .build()
                .writeToPath(Path.of(build_dir));
    }

    private static ParameterizedTypeName makeStreamObserverType(ClassName responseMessage) {
        return ParameterizedTypeName.get(ClassName.get(StreamObserver.class), responseMessage);
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
                    ClassName.bestGuess(String.format("%s.%s", build_package, schemaDescriptor.getClassName())),
                    tableInstanceVariableName)
                .build()
                .writeToPath(Path.of(build_dir));
    }

}
