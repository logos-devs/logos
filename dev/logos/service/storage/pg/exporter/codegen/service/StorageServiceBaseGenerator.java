package dev.logos.service.storage.pg.exporter.codegen.service;

import com.google.inject.Inject;
import com.squareup.javapoet.*;
import dev.logos.service.storage.EntityStorage;
import dev.logos.service.storage.EntityStorageService;
import dev.logos.service.storage.pg.Converter;
import dev.logos.service.storage.pg.Select;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.SchemaDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.TableDescriptor;
import dev.logos.service.storage.validator.Validator;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;

import static javax.lang.model.element.Modifier.*;

public class StorageServiceBaseGenerator {

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
                        .addMethod(generateQueryMethod(tableDescriptor.name(), 
                           schemaDescriptor.qualifiers() != null && 
                           schemaDescriptor.qualifiers().containsKey(tableDescriptor.name()) &&
                           !schemaDescriptor.qualifiers().get(tableDescriptor.name()).isEmpty()))
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
    
    /**
     * Generates the query method with qualifier support.
     */
    private MethodSpec generateQueryMethod(String tableName, boolean hasQualifiers) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("query")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addTypeVariable(TypeVariableName.get("Request"))
                .addParameter(TypeVariableName.get("Request"), "request")
                .returns(ClassName.get("dev.logos.service.storage.pg", "Select.Builder"));
                
        // Build core code
        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                .addStatement("$T builder = $T.builder().from($S)", 
                        ClassName.get("dev.logos.service.storage.pg", "Select.Builder"),
                        ClassName.get("dev.logos.service.storage.pg", "Select"),
                        tableName);
        
        // Basic limit/offset handling for list requests
        codeBuilder.beginControlFlow("if (request instanceof ListRequest listRequest)");
        codeBuilder.addStatement("Long limit = listRequest.getLimit()");
        codeBuilder.addStatement("Long offset = listRequest.getOffset()");
        codeBuilder.beginControlFlow("if (limit > 0)");
        codeBuilder.addStatement("builder.limit(limit)");
        codeBuilder.endControlFlow();
        codeBuilder.beginControlFlow("if (offset > 0)");
        codeBuilder.addStatement("builder.offset(offset)");
        codeBuilder.endControlFlow();
        
        // Add qualifier handling if table has qualifiers
        if (hasQualifiers) {
            codeBuilder.add("\n// Process qualifier functions if present\n");
            codeBuilder.addStatement("try {\n  java.lang.reflect.Method getQualifiersMethod = request.getClass().getMethod(\"getListQualifiersList\");\n  java.util.List<?> qualifiers = (java.util.List<?>) getQualifiersMethod.invoke(request);\n  \n  if (qualifiers != null && !qualifiers.isEmpty()) {\n    for (Object qualifier : qualifiers) {\n      // Extract qualifier name from class name\n      String qualifierName = qualifier.getClass().getSimpleName();\n      \n      // Extract parameters\n      java.util.Map<String, Object> params = new java.util.HashMap<>();\n      java.lang.reflect.Method[] methods = qualifier.getClass().getMethods();\n      \n      for (java.lang.reflect.Method method : methods) {\n        String methodName = method.getName();\n        \n        if (methodName.startsWith(\"get\") && \n            !methodName.equals(\"getClass\") &&\n            !methodName.equals(\"getDefaultInstanceForType\") &&\n            method.getParameterCount() == 0) {\n          \n          String paramName = methodName.substring(3);\n          paramName = Character.toLowerCase(paramName.charAt(0)) + paramName.substring(1);\n          \n          Object value = method.invoke(qualifier);\n          if (value != null) {\n            params.put(paramName, value);\n          }\n        }\n      }\n      \n      // Add qualifier to builder\n      builder.qualifier(qualifierName, params);\n    }\n  }\n} catch (Exception e) {\n  // No qualifiers or error processing them\n}");
        }
        codeBuilder.endControlFlow();
        
        codeBuilder.add("\nreturn builder;\n");
        
        return methodBuilder.addCode(codeBuilder.build()).build();
    }
}