package dev.logos.service.storage.pg.exporter.codegen.proto;

import com.google.inject.Inject;
import dev.logos.service.storage.pg.exporter.descriptor.FunctionDescriptor;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.*;
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

        // file header + imports
        proto.append(protoHeader(targetPackage, functionDescriptors)).append("\n");

        // 1) shared SETOF-composite messages
        Map<String, FunctionDescriptor> composites = functionDescriptors.stream()
                .flatMap(fd -> fd.protoResponseMessageName()
                        .map(name -> Map.entry(name, fd))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));

        for (var entry : composites.entrySet()) {
            String msgName = entry.getKey();
            FunctionDescriptor sample = entry.getValue();
            List<String> fields = sample.returnType().stream()
                    .map(p -> getPgTypeMapper(p.type()).protoField(p.name()))
                    .collect(Collectors.toList());
            proto.append(protoMessage(msgName, fields)).append("\n");
        }

        // 2) per-function request & (maybe) response
        for (FunctionDescriptor fd : functionDescriptors) {
            // request
            proto.append(protoMessage(
                    fd.requestMessageClassName(),
                    fd.parameters().stream()
                            .map(p -> getPgTypeMapper(p.type()).protoField(p.name()))
                            .collect(Collectors.toList())
            )).append("\n");

            // response only if not shared
            if (fd.protoResponseMessageName().isEmpty()) {
                proto.append(protoMessage(
                        fd.responseMessageClassName(),
                        fd.returnType().stream()
                                .map(p -> getPgTypeMapper(p.type()).protoField(p.name()))
                                .collect(Collectors.toList())
                )).append("\n");
            }
        }

        // 3) service RPCs
        proto.append(protoService(serviceName, functionDescriptors));

        return proto.toString();
    }

    private PgTypeMapper getPgTypeMapper(String type) {
        PgTypeMapper mapper = pgColumnTypeMappers.get(type);
        if (mapper == null) {
            throw new RuntimeException("No PgTypeMapper bound for type: " + type);
        }
        return mapper;
    }

    private String protoHeader(String targetPackage, List<FunctionDescriptor> functionDescriptors) {
        String imports = functionDescriptors.stream()
                .flatMap(fd -> Stream.concat(fd.parameters().stream(), fd.returnType().stream()))
                .flatMap(p -> getPgTypeMapper(p.type()).protoImports().stream())
                .map(path -> "import \"" + path.replace("\"", "") + "\";")
                .distinct()
                .collect(Collectors.joining("\n"));

        return String.format("""
                syntax = "proto3";
                
                option java_package = "%s";
                option java_multiple_files = true;
                
                %s
                """, targetPackage, imports);
    }

    private String protoMessage(String messageName, List<String> fields) {
        StringBuilder msg = new StringBuilder();
        msg.append("message ").append(messageName).append(" {\n");
        int fieldNumber = 1;
        for (String field : fields) {
            msg.append("  ").append(field).append(" = ").append(fieldNumber++).append(";\n");
        }
        msg.append("}\n");
        return msg.toString();
    }

    private String protoService(String serviceName, List<FunctionDescriptor> functionDescriptors) {
        StringBuilder svc = new StringBuilder();
        svc.append("service ").append(serviceName).append(" {\n");
        for (FunctionDescriptor fd : functionDescriptors) {
            String resp = fd.protoResponseMessageName().orElse(fd.responseMessageClassName());
            svc.append("  rpc ")
                    .append(fd.rpcMethodName())
                    .append("(").append(fd.requestMessageClassName()).append(") ")
                    .append("returns (stream ").append(resp).append(");\n");
        }
        svc.append("}\n");
        return svc.toString();
    }
}