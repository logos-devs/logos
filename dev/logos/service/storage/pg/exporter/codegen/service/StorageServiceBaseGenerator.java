package dev.logos.service.storage.pg.exporter.codegen.service;

import com.google.inject.Inject;
import com.squareup.javapoet.*;
import dev.logos.service.storage.EntityStorage;
import dev.logos.service.storage.EntityStorageService;
import dev.logos.service.storage.pg.Converter;
import dev.logos.service.storage.pg.Identifier;
import dev.logos.service.storage.pg.Select;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierParameterDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.SchemaDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.TableDescriptor;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;
import dev.logos.service.storage.validator.Validator;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static javax.lang.model.element.Modifier.*;

public class StorageServiceBaseGenerator {
    private final Map<String, PgTypeMapper> pgColumnTypeMappers;

    @Inject
    public StorageServiceBaseGenerator(Map<String, PgTypeMapper> pgColumnTypeMappers) {
        this.pgColumnTypeMappers = pgColumnTypeMappers;
    }

    PgTypeMapper getPgTypeMapper(String type) {
        if (!pgColumnTypeMappers.containsKey(type)) {
            throw new RuntimeException("There is no PgTypeMapper bound for type: " + type);
        }

        return pgColumnTypeMappers.get(type);
    }

    public JavaFile generate(String targetPackage,
                             SchemaDescriptor schemaDescriptor,
                             TableDescriptor tableDescriptor) throws IOException {
        String packageName = targetPackage + "." + schemaDescriptor.name();
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
        
        // Create table-specific QualifierCall class name for this package
        ClassName qualifierCallClassName = ClassName.bestGuess(
                String.format("%s.%sQualifierCall", packageName, tableClassName.simpleName()));

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
                        .addMethod(generateQueryMethod(
                                tableDescriptor,
                                listRequestMessage,
                                qualifierCallClassName,
                                packageName))
                        .addMethod(makeIdGetter(getRequestMessage, updateRequestMessage, deleteRequestMessage))

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
                                          .addStatement("return (Response) $T.newBuilder().setId($T.uuidToBytestring(id)).build()", createResponseMessage, Converter.class)
                                          .endControlFlow()
                                          .addStatement("return null")
                                          .build());

        storageServiceClassSpec.addMethod(makeValidateRequestMethod(listRequestMessage, false))
                               .addMethod(makeValidateRequestMethod(createRequestMessage, true))
                               .addMethod(makeValidateRequestMethod(updateRequestMessage, true))
                               .addMethod(makeValidateRequestMethod(deleteRequestMessage, false));

        // entity-level validator that applies to creates and updates
        storageServiceClassSpec.addMethod(makeValidateEntityMethod(entityMessage));

        return JavaFile.builder(packageName, storageServiceClassSpec.build())
                       .addStaticImport(
                               ClassName.bestGuess(String.format("%s.%s", targetPackage, schemaDescriptor.getClassName())),
                               tableDescriptor.getInstanceVariableName())
                       .build();
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

    private ParameterizedTypeName makeStreamObserverType(ClassName responseMessage) {
        return ParameterizedTypeName.get(ClassName.get(StreamObserver.class), responseMessage);
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
            ClassName getRequestMessage,
            ClassName updateRequestMessage,
            ClassName deleteRequestMessage) {

        TypeVariableName requestType = TypeVariableName.get("Request");

        return MethodSpec.methodBuilder("id")
                         .addModifiers(PUBLIC)
                         .addTypeVariable(requestType)
                         .addParameter(requestType, "request")
                         .returns(Object.class)
                         .addStatement("""
                                         return switch (request) {
                                             case $T getRequest -> getRequest.getId();
                                             case $T updateRequest -> updateRequest.getId();
                                             case $T deleteRequest -> deleteRequest.getId();
                                             default -> throw new $T($S);
                                         }
                                         """,
                                 getRequestMessage,
                                 updateRequestMessage,
                                 deleteRequestMessage,
                                 RuntimeException.class,
                                 "Unexpected request type"
                         ).build();
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

    public MethodSpec generateQueryMethod(
            TableDescriptor table,
            ClassName listRequestMessage,
            ClassName qualifierCallClassName,
            String qualifierJavaPackage
    ) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("query")
                                                     .addAnnotation(Override.class)
                                                     .addModifiers(PUBLIC, FINAL)
                                                     .addTypeVariable(TypeVariableName.get("Request"))
                                                     .addParameter(TypeVariableName.get("Request"), "request")
                                                     .returns(ClassName.get("dev.logos.service.storage.pg", "Select.Builder"));

        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                                                 .addStatement("$T builder = $T.builder().from($L)",
                                                         ClassName.get("dev.logos.service.storage.pg", "Select.Builder"),
                                                         ClassName.get("dev.logos.service.storage.pg", "Select"),
                                                         table.getInstanceVariableName());

        codeBuilder.beginControlFlow("if (request instanceof $T listRequest)", listRequestMessage);
        
        // Add pagination handling
        codeBuilder.beginControlFlow("if (listRequest.hasLimit())");
        codeBuilder.addStatement("builder.limit(listRequest.getLimit())");
        codeBuilder.endControlFlow();
        codeBuilder.add("\n");
        codeBuilder.beginControlFlow("if (listRequest.hasOffset())");
        codeBuilder.addStatement("builder.offset(listRequest.getOffset())");
        codeBuilder.endControlFlow();
        codeBuilder.add("\n");

        // Process qualifiers using the QualifierCall wrapper
        if (!table.qualifierDescriptors().isEmpty()) {
            codeBuilder.beginControlFlow("for ($T qualifierCall : listRequest.getQualifierCallList())", qualifierCallClassName);
            codeBuilder.beginControlFlow("switch (qualifierCall.getQualifierCase())");
            
            // Generate a case for each qualifier
            int index = 0;
            for (QualifierDescriptor qualifier : table.qualifierDescriptors()) {
                String qualifierMessageName = Identifier.snakeToCamelCase(qualifier.name());
                String qualifierSnakeCase = Identifier.camelToSnakeCase(qualifier.name());
                
                // The oneof case name will be in snake_case but uppercased
                String qualifierCaseName = qualifierSnakeCase.toUpperCase();
                
                // Start case block - use index for unique variable names
                index++;
                String varName = "qualifier" + index;
                codeBuilder.addStatement("case $L:", qualifierCaseName);
                codeBuilder.addStatement("var $L = qualifierCall.get$L()", varName, qualifierMessageName);
                
                // Create parameter map
                String paramMapVar = qualifier.getInstanceVariableName() + "Params";
                codeBuilder.addStatement(
                        "$T $L = new $T<>()",
                        ParameterizedTypeName.get(
                                ClassName.get(LinkedHashMap.class),
                                ClassName.get(String.class),
                                ClassName.get(Object.class)),
                        paramMapVar,
                        LinkedHashMap.class
                );
                
                // Add each parameter
                for (QualifierParameterDescriptor param : qualifier.parameters()) {
                    String paramName = param.name();
                    String getterRoot = "get" + Identifier.snakeToCamelCase(
                        Character.toUpperCase(paramName.charAt(0)) + paramName.substring(1));
                    
                    PgTypeMapper mapper = getPgTypeMapper(param.type());
                    boolean isRepeated = mapper.protoFieldRepeated();
                    String getterCall = varName + "." + getterRoot + (isRepeated ? "List()" : "()");
                    
                    codeBuilder.addStatement("$L.put($S, $L)", paramMapVar, paramName, getterCall);
                }
                
                // Add qualifier to builder
                codeBuilder.addStatement("builder.qualifier($L.$L, $L)", 
                                        table.getInstanceVariableName(), 
                                        qualifier.getInstanceVariableName(), 
                                        paramMapVar);
                codeBuilder.addStatement("break");
            }
            
            // Add default case for QUALIFIER_NOT_SET
            codeBuilder.addStatement("case QUALIFIER_NOT_SET:");
            codeBuilder.addStatement("// No qualifier set, nothing to do");
            codeBuilder.addStatement("break");
            
            // End switch and for loop
            codeBuilder.endControlFlow();
            codeBuilder.endControlFlow();
        }

        codeBuilder.endControlFlow(); // End if request instanceof ListRequest

        // Let subclasses modify the builder
        codeBuilder.addStatement("builder = query(request, builder)");
        codeBuilder.add("return builder;\n");

        return methodBuilder.addCode(codeBuilder.build()).build();
    }
}