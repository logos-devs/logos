package dev.logos.service.storage.pg.exporter.codegen.proto;

import com.google.inject.Inject;
import dev.logos.service.storage.pg.exporter.descriptor.FunctionDescriptor;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProtoGenerator {
    private final Map<String, PgTypeMapper> pgColumnTypeMappers;

    @Inject
    public ProtoGenerator(Map<String, PgTypeMapper> pgColumnTypeMappers) {
        this.pgColumnTypeMappers = pgColumnTypeMappers;
    }

    public String generate(String targetPackage, String serviceName, List<FunctionDescriptor> functionDescriptors) {
        StringBuilder proto = new StringBuilder();
        proto.append(protoHeader(targetPackage, functionDescriptors));

        for (FunctionDescriptor functionDescriptor : functionDescriptors) {
            // request message
            proto.append(protoMessage(
                    functionDescriptor.requestMessageClassName(),
                    functionDescriptor.parameters().stream()
                                      .map(param -> getPgTypeMapper(param.type()).protoField(param.name()))
                                      .collect(Collectors.toList())
            ));

            // response message
            proto.append(protoMessage(
                    functionDescriptor.responseMessageClassName(),
                    functionDescriptor.returnType().stream().map(
                            functionParameterDescriptor -> getPgTypeMapper(functionParameterDescriptor.type()).protoField(
                                    functionParameterDescriptor.name()
                            )
                    ).toList()
            ));
        }

        proto.append(protoService(serviceName, functionDescriptors));

        return proto.toString();
    }

    PgTypeMapper getPgTypeMapper(String type) {
        if (!pgColumnTypeMappers.containsKey(type)) {
            throw new RuntimeException("There is no PgTypeMapper bound for type: " + type);
        }

        return pgColumnTypeMappers.get(type);
    }

    private String protoMessage(String messageName, Iterable<String> fields) {
        StringBuilder message = new StringBuilder();
        message.append("message ").append(messageName).append(" {\n");

        int fieldNumber = 1;
        for (String field : fields) {
            message.append("  ").append(field).append(" = ").append(fieldNumber++).append(";\n");
        }

        message.append("}\n");
        return message.toString();
    }

    private String protoHeader(String targetPackage, List<FunctionDescriptor> functionDescriptors) {
        String imports = functionDescriptors
                .stream()
                .flatMap(functionDescriptor ->
                        Stream.concat(functionDescriptor.parameters().stream(), functionDescriptor.returnType().stream()))
                .flatMap(functionParameterDescriptor -> getPgTypeMapper(functionParameterDescriptor.type())
                        .protoImports()
                        .stream())
                .map(importPath -> importPath.replaceAll("\"", ""))
                .distinct()
                .map(path -> "import \"" + path + "\";")
                .collect(Collectors.joining("\n"));

        return """
                syntax = "proto3";
                
                option java_package = "%s";
                option java_multiple_files = true;
                
                %s
                """.formatted(targetPackage, imports);
    }


    private String protoService(String serviceName, List<FunctionDescriptor> functionDescriptors) {
        return """
                service %s {
                %s
                }
                """.formatted(serviceName,
                functionDescriptors.stream().map(
                        functionDescriptor -> String.format(
                                "  rpc %s(%s) returns (stream %s);",
                                functionDescriptor.rpcMethodName(),
                                functionDescriptor.requestMessageClassName(),
                                functionDescriptor.responseMessageClassName()
                        )
                ).collect(Collectors.joining("\n")));
    }
}