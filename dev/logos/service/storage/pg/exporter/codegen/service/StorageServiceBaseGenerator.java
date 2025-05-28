package dev.logos.service.storage.pg.exporter.codegen.service;

import com.google.inject.Inject;
import com.squareup.javapoet.*;
import dev.logos.service.Service;
import dev.logos.service.storage.pg.exporter.descriptor.FunctionDescriptor;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;
import dev.logos.service.storage.validator.Validator;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.StatementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static dev.logos.service.storage.pg.exporter.descriptor.ExportedIdentifier.snakeToCamelCase;
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

    private MethodSpec makeValidateRequestMethod(ClassName requestMessage) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("validate")
                                              .addModifiers(PROTECTED)
                                              .addParameter(requestMessage, "request")
                                              .addParameter(ClassName.get(Validator.class), "validator");
        return method.build();
    }

    /*
    protected void message_list(
        MessageListRequest request,
        StreamObserver<MessageListResponse> responseObserver
       ) {
               String sqlQuery = "...";

               try {
                   Handle handle = jdbi.open();
                   Query query = handle.createQuery(sqlQuery);

                   // bindFields(...)

                   return query.map((ResultSet rs, StatementContext ctx) -> {
                       // mapResultFields(...)
                   })
                   .stream()
                   .onClose(handle::close);
               } catch (SQLException e) {
                   logger.log(Level.SEVERE, "Failed to execute query %s".formatted(sqlQuery), e);
                   responseObserver.onError(
                           Status.INTERNAL.withDescription("Failed to execute query")
                                         .withCause(e)
                                         .asRuntimeException()
                   );
               }
         }
     */
    MethodSpec makeRpcHandler(FunctionDescriptor functionDescriptor, ClassName requestMessage, ClassName responseMessage) {
        MethodSpec.Builder rpcHandlerBuilder =
                MethodSpec.methodBuilder(functionDescriptor.rpcMethodName())
                          .addModifiers(PUBLIC)
                          .addParameter(requestMessage, "request")
                          .addParameter(ParameterizedTypeName.get(ClassName.get(StreamObserver.class), responseMessage),
                                  "responseObserver")
                          .addStatement("$T handle = jdbi.open()", Handle.class)
                          .addStatement("$T query = handle.createQuery($S)", Query.class, functionDescriptor.toSql());

        functionDescriptor.parameters().forEach(functionParameterDescriptor -> {
            String parameterName = functionParameterDescriptor.name();
            String parameterType = functionParameterDescriptor.type();
            PgTypeMapper pgTypeMapper = getPgTypeMapper(parameterType);

            rpcHandlerBuilder
                    .addStatement(
                            pgTypeMapper.protoToPg(
                                    "query",
                                    parameterName,
                                    "request",
                                    "get" + functionParameterDescriptor.protoMethodName() + (pgTypeMapper.protoFieldRepeated() ? "List" : "")));
        });

        rpcHandlerBuilder
                .beginControlFlow("query.map(($T resultSet, $T ctx) -> ", ResultSet.class, StatementContext.class)
                .addStatement("$T.Builder builder = $T.newBuilder()", responseMessage, responseMessage)
                .beginControlFlow("try");

        functionDescriptor.returnType().forEach(
                functionParameterDescriptor -> {
                    String fieldName = functionParameterDescriptor.name();
                    String fieldType = functionParameterDescriptor.type();
                    String protoMethodName = functionParameterDescriptor.protoMethodName();

                    PgTypeMapper typeMapper = getPgTypeMapper(fieldType);
                    String setterName = "%s%s".formatted(
                            typeMapper.protoFieldRepeated() ? "addAll" : "set",
                            protoMethodName
                    );

                    rpcHandlerBuilder.beginControlFlow("if (resultSet.getObject($S) != null)", fieldName)
                                     .addStatement("builder.$N($L)",
                                             setterName
                                             ,
                                             typeMapper.pgToProto(
                                                     CodeBlock.of(
                                                             "$L resultSet.$N($S)",
                                                             typeMapper.resultSetFieldCast(),
                                                             typeMapper.resultSetFieldGetter(),
                                                             fieldName)))
                                     .endControlFlow();
                }
        );

        rpcHandlerBuilder
                .addStatement("return builder.build()")
                .endControlFlow()
                .beginControlFlow("catch ($T e)", Throwable.class)
                .addStatement("logger.atError().setCause(e).addKeyValue(\"query\", $S).log(\"Failed to map result set\")",
                        functionDescriptor.toSql())
                .addStatement("""
                        responseObserver.onError($T.INTERNAL.withDescription("Failed to execute query")
                                        .withCause(e)
                                        .asRuntimeException())
                        """, Status.class)
                .addStatement("throw new $T(e)", RuntimeException.class)
                .endControlFlow()
                .endControlFlow()
                .addStatement(").stream().forEach(responseObserver::onNext)")
                .addStatement("query.close()")
                .addStatement("handle.close()")
                .addStatement("responseObserver.onCompleted()");

        return rpcHandlerBuilder.build();
    }

    public TypeSpec generate(String targetPackage, String serviceName, List<FunctionDescriptor> functionDescriptors) {
        System.err.println(this.pgColumnTypeMappers.keySet());
        ClassName serviceClassName = ClassName.bestGuess(String.format("%s.%s", targetPackage, serviceName));

        TypeSpec.Builder storageServiceBuilder =
                TypeSpec.classBuilder(String.format("%sBase", serviceClassName.simpleName()))
                        .addModifiers(PUBLIC, ABSTRACT)
                        .superclass(ClassName.bestGuess(
                                String.format("%s.%sGrpc.%sImplBase", targetPackage,
                                        serviceClassName.simpleName(),
                                        serviceClassName.simpleName())))
                        .addSuperinterface(Service.class)
                        .addMethod(MethodSpec.constructorBuilder()
                                             .addModifiers(PUBLIC)
                                             .build())
                        .addField(FieldSpec.builder(Jdbi.class, "jdbi", PROTECTED)
                                           .addAnnotation(Inject.class)
                                           .build())
                        .addField(FieldSpec.builder(Logger.class, "logger", FINAL)
                                           .initializer("$T.getLogger($TBase.class)", LoggerFactory.class, serviceClassName)
                                           .build());

        for (FunctionDescriptor functionDescriptor : functionDescriptors) {
            String functionName = snakeToCamelCase(functionDescriptor.name());
            ClassName requestMessage = ClassName.bestGuess(String.format("%s.%sRequest", targetPackage, functionName));
            ClassName responseMessage = ClassName.bestGuess(String.format("%s.%sResponse", targetPackage, functionName));

            storageServiceBuilder.addMethod(makeRpcHandler(functionDescriptor, requestMessage, responseMessage))
                                 .addMethod(makeValidateRequestMethod(requestMessage));

        }

        return storageServiceBuilder.build();
    }
}