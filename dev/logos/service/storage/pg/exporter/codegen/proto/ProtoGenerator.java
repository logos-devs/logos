package dev.logos.service.storage.pg.exporter.codegen.proto;

import com.google.inject.Inject;
import dev.logos.service.storage.pg.exporter.descriptor.ColumnDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.SchemaDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.TableDescriptor;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProtoGenerator {
    private final Map<String, PgTypeMapper> pgColumnTypeMappers;

    @Inject
    public ProtoGenerator(Map<String, PgTypeMapper> pgColumnTypeMappers) {
        this.pgColumnTypeMappers = pgColumnTypeMappers;
    }

    public String generate(String targetPackage,
                           SchemaDescriptor schemaDescriptor,
                           TableDescriptor tableDescriptor) {
        String tableSimpleName = tableDescriptor.getClassName().simpleName();

        return String.join("\n",
                List.of(
                        protoHeader(targetPackage, schemaDescriptor, tableDescriptor),
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

    PgTypeMapper getPgTypeMapper(String type) {
        if (!pgColumnTypeMappers.containsKey(type)) {
            throw new RuntimeException("There is no PgTypeMapper bound for type: " + type);
        }

        return pgColumnTypeMappers.get(type);
    }

    private String protoHeader(String targetPackage, SchemaDescriptor schemaDescriptor, TableDescriptor tableDescriptor) {
        String packageName = targetPackage + "." + schemaDescriptor.name();
        String imports = tableDescriptor
                .columns()
                .stream()
                .flatMap(columnDescriptor -> pgColumnTypeMappers.get(columnDescriptor.type()).protoImports().stream())
                .map(importPath -> importPath.replaceAll("\"", ""))
                .map("import \"%s\";"::formatted).collect(Collectors.joining("\n"));

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
}
