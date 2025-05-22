package dev.logos.service.storage.pg.exporter.codegen.proto;

import com.google.inject.Inject;
import dev.logos.service.storage.pg.exporter.descriptor.ColumnDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.DerivedFieldDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.DerivedFieldParameterDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierParameterDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.SchemaDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.TableDescriptor;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;
import dev.logos.service.storage.pg.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        List<String> messages = new ArrayList<>(List.of(
                protoHeader(targetPackage, schemaDescriptor, tableDescriptor),
                protoCreateRequestMessage(tableSimpleName),
                protoCreateResponseMessage(tableSimpleName),
                protoGetRequestMessage(tableSimpleName),
                protoGetResponseMessage(tableSimpleName),
                protoUpdateRequestMessage(tableSimpleName),
                protoUpdateResponseMessage(tableSimpleName),
                protoDeleteRequestMessage(tableSimpleName),
                protoDeleteResponseMessage(tableSimpleName),
                protoListRequestMessage(tableSimpleName, tableDescriptor),
                protoListResponseMessage(tableSimpleName),
                protoEntityMessage(tableSimpleName, tableDescriptor.columns(), tableDescriptor.derivedFieldDescriptors())
        ));
        
        // Generate qualifier messages if there are qualifiers
        if (!tableDescriptor.qualifierDescriptors().isEmpty()) {
            messages.add(protoQualifierCallMessage(tableDescriptor));

            // Generate individual qualifier messages
            for (QualifierDescriptor qualifier : tableDescriptor.qualifierDescriptors()) {
                messages.add(protoQualifierMessage(qualifier));
            }
        }
        
        // Generate derived field messages if there are derived fields
        if (!tableDescriptor.derivedFieldDescriptors().isEmpty()) {
            messages.add(protoDerivedFieldCallMessage(tableDescriptor));

            // Generate individual derived field messages
            for (DerivedFieldDescriptor derivedField : tableDescriptor.derivedFieldDescriptors()) {
                messages.add(protoDerivedFieldMessage(derivedField));
            }
            
            // Generate the derived field value wrapper message with all used return types
            messages.add(protoDerivedFieldValueMessage(tableSimpleName, tableDescriptor.derivedFieldDescriptors()));
        }
        
        // Add service definition last
        messages.add(protoService(tableSimpleName));
        
        return String.join("\n", messages);
    }

    PgTypeMapper getPgTypeMapper(String type) {
        if (!pgColumnTypeMappers.containsKey(type)) {
            throw new RuntimeException("There is no PgTypeMapper bound for type: " + type);
        }

        return pgColumnTypeMappers.get(type);
    }
    
    /**
     * Generate a QualifierCall message for this table that wraps all qualifiers in a oneof.
     */
    private String protoQualifierCallMessage(TableDescriptor tableDescriptor) {
        String tableSimpleName = tableDescriptor.getClassName().simpleName();
        
        // Generate the oneof block containing all qualifiers
        StringBuilder oneofBody = new StringBuilder();
        int fieldNumber = 1;
        
        for (QualifierDescriptor qualifier : tableDescriptor.qualifierDescriptors()) {
            String qualifierName = Identifier.snakeToCamelCase(qualifier.name());
            String fieldName = Identifier.camelToSnakeCase(qualifier.name());
            oneofBody.append(String.format("    %s %s = %d;\n", 
                             qualifierName, 
                             fieldName, 
                             fieldNumber++));
        }
        
        return """
               message %sQualifierCall {
                 oneof qualifier {
               %s  }
               }
               """.formatted(tableSimpleName, oneofBody);
    }
    
    /**
     * Generate a DerivedFieldCall message for this table that wraps all derived fields in a oneof.
     */
    private String protoDerivedFieldCallMessage(TableDescriptor tableDescriptor) {
        String tableSimpleName = tableDescriptor.getClassName().simpleName();
        
        // Generate the oneof block containing all derived fields
        StringBuilder oneofBody = new StringBuilder();
        int fieldNumber = 1;
        
        for (DerivedFieldDescriptor derivedField : tableDescriptor.derivedFieldDescriptors()) {
            String derivedFieldName = Identifier.snakeToCamelCase(derivedField.name());
            String fieldName = Identifier.camelToSnakeCase(derivedField.name());
            oneofBody.append(String.format("    %s %s = %d;\n", 
                             derivedFieldName, 
                             fieldName, 
                             fieldNumber++));
        }
        
        return """
               message %sDerivedFieldCall {
                 oneof derived_field {
               %s  }
               }
               """.formatted(tableSimpleName, oneofBody);
    }
    
    /**
     * Generate a DerivedFieldValue message for this table that wraps all possible return types
     * from the derived fields in a oneof.
     */
    private String protoDerivedFieldValueMessage(String entityName, List<DerivedFieldDescriptor> derivedFields) {
        // Get unique return types
        Set<String> uniqueReturnTypes = getUniqueDerivedFieldReturnTypes(derivedFields);
        
        // Generate oneof fields for each unique type
        StringBuilder oneofBody = new StringBuilder();
        int fieldNumber = 1;
        
        for (String returnType : uniqueReturnTypes) {
            PgTypeMapper typeMapper = getPgTypeMapper(returnType);
            String fieldTypeName = typeMapper.protoFieldTypeKeyword();
            String fieldName = Identifier.camelToSnakeCase(fieldTypeName) + "_value";
            
            oneofBody.append(String.format("    %s%s %s = %d;\n", 
                             typeMapper.protoFieldRepeated() ? "repeated " : "",
                             fieldTypeName, 
                             fieldName, 
                             fieldNumber++));
        }
        
        return """
               message %sDerivedFieldValue {
                 oneof value {
               %s  }
               }
               """.formatted(entityName, oneofBody);
    }
    
    /**
     * Get the set of unique return types used by all derived fields.
     */
    private Set<String> getUniqueDerivedFieldReturnTypes(List<DerivedFieldDescriptor> derivedFields) {
        // Use LinkedHashSet to maintain insertion order for consistent field numbers
        return derivedFields.stream()
                .map(DerivedFieldDescriptor::returnType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    
    /**
     * Generate a message for a specific qualifier function
     */
    private String protoQualifierMessage(QualifierDescriptor qualifier) {
        String qualifierName = Identifier.snakeToCamelCase(qualifier.name());
        StringBuilder fields = new StringBuilder();
        int fieldNumber = 1;
        
        for (QualifierParameterDescriptor param : qualifier.parameters()) {
            PgTypeMapper typeMapper = getPgTypeMapper(param.type());
            fields.append(String.format("  %s%s %s = %d;\n",
                         typeMapper.protoFieldRepeated() ? "repeated " : "",
                         typeMapper.protoFieldTypeKeyword(),
                         param.name(),
                         fieldNumber++));
        }
        
        return """
               message %s {
               %s}
               """.formatted(qualifierName, fields);
    }
    
    /**
     * Generate a message for a specific derived field function
     */
    private String protoDerivedFieldMessage(DerivedFieldDescriptor derivedField) {
        String derivedFieldName = Identifier.snakeToCamelCase(derivedField.name());
        StringBuilder fields = new StringBuilder();
        int fieldNumber = 1;
        
        for (DerivedFieldParameterDescriptor param : derivedField.parameters()) {
            PgTypeMapper typeMapper = getPgTypeMapper(param.type());
            fields.append(String.format("  %s%s %s = %d;\n",
                         typeMapper.protoFieldRepeated() ? "repeated " : "",
                         typeMapper.protoFieldTypeKeyword(),
                         param.name(),
                         fieldNumber++));
        }
        
        return """
               message %s {
               %s}
               """.formatted(derivedFieldName, fields);
    }

    private String protoHeader(String targetPackage, SchemaDescriptor schemaDescriptor, TableDescriptor tableDescriptor) {
        String packageName = targetPackage + "." + schemaDescriptor.name();
        
        // Collect imports from column types
        List<String> importPaths = tableDescriptor
                .columns()
                .stream()
                .flatMap(columnDescriptor -> pgColumnTypeMappers.get(columnDescriptor.type()).protoImports().stream())
                .map(importPath -> importPath.replaceAll("\"", ""))
                .collect(Collectors.toList());
        
        // Add imports for derived field return types
        if (!tableDescriptor.derivedFieldDescriptors().isEmpty()) {
            Set<String> uniqueReturnTypes = getUniqueDerivedFieldReturnTypes(tableDescriptor.derivedFieldDescriptors());
            for (String returnType : uniqueReturnTypes) {
                PgTypeMapper typeMapper = getPgTypeMapper(returnType);
                importPaths.addAll(typeMapper.protoImports().stream()
                        .map(importPath -> importPath.replaceAll("\"", ""))
                        .collect(Collectors.toList()));
            }
        }
        
        // Format imports
        String imports = importPaths.stream()
                .distinct()
                .map(path -> "import \"" + path + "\";")
                .collect(Collectors.joining("\n"));

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

    private String protoListRequestMessage(String entityName, TableDescriptor tableDescriptor) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                message List%sRequest {
                    optional int64 limit = 1;
                    optional int64 offset = 2;
                """.formatted(entityName));

        // Add qualifier_call field if there are qualifiers
        if (!tableDescriptor.qualifierDescriptors().isEmpty()) {
            builder.append("    repeated %sQualifierCall qualifier_call = 3;\n".formatted(entityName));
        }
        
        // Add derived_field_call field if there are derived fields
        if (!tableDescriptor.derivedFieldDescriptors().isEmpty()) {
            builder.append("    repeated %sDerivedFieldCall derived_field_call = 4;\n".formatted(entityName));
        }

        builder.append("}\n");
        return builder.toString();
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

    private String protoEntityMessage(String entityName, List<ColumnDescriptor> columnDescriptors, List<DerivedFieldDescriptor> derivedFieldDescriptors) {
        // Generate regular column fields
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
        
        // Add derived fields map if derived fields exist
        if (!derivedFieldDescriptors.isEmpty()) {
            int nextFieldNumber = columnDescriptors.size() + 1;
            fields += "\n    map<string, %sDerivedFieldValue> derived_fields = %d;".formatted(
                    entityName, nextFieldNumber);
        }
        
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