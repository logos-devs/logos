package dev.logos.service.storage.pg.exporter.codegen.proto;

import com.google.inject.Inject;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierParameterDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierDescriptor;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generator for qualifier-related Proto definitions.
 */
public class QualifierProtoGenerator {
    private final Map<String, PgTypeMapper> pgTypeMappers;

    @Inject
    public QualifierProtoGenerator(Map<String, PgTypeMapper> pgTypeMappers) {
        this.pgTypeMappers = pgTypeMappers;
    }

    /**
     * Generates Proto message definitions for qualifiers.
     *
     * @param qualifiers List of qualifier descriptors
     * @return Proto message definitions for qualifiers
     */
    public String generateQualifierMessages(List<QualifierDescriptor> qualifiers) {
        if (qualifiers == null || qualifiers.isEmpty()) {
            return "";
        }

        List<String> messages = new ArrayList<>();
        for (QualifierDescriptor qualifier : qualifiers) {
            messages.add(generateQualifierMessage(qualifier));
        }

        return String.join("\n\n", messages);
    }

    /**
     * Generates a Proto message definition for a single qualifier.
     *
     * @param qualifier Qualifier descriptor
     * @return Proto message definition
     */
    private String generateQualifierMessage(QualifierDescriptor qualifier) {
        StringBuilder fieldsBuilder = new StringBuilder();

        // Add fields for each parameter
        int fieldNumber = 1;
        for (QualifierParameterDescriptor param : qualifier.parameters()) {
            PgTypeMapper typeMapper = getTypeMapper(param.type());

            fieldsBuilder.append("  ")
                         .append(typeMapper.protoFieldRepeated() ? "repeated " : "")
                         .append(typeMapper.protoFieldTypeKeyword())
                         .append(" ")
                         .append(param.name())
                         .append(" = ")
                         .append(fieldNumber++)
                         .append(";\n");
        }

        return String.format("message %s {\n%s}",
                qualifier.getClassName().simpleName(),
                fieldsBuilder.toString());
    }

    /**
     * Generates additions to request messages to include qualifier fields.
     *
     * @param requestType Type of request (List, Update, Delete)
     * @param qualifiers  List of qualifier descriptors
     * @return Proto field definitions to add to the request message
     */
    public String generateQualifierFields(String requestType, List<QualifierDescriptor> qualifiers) {
        if (qualifiers == null || qualifiers.isEmpty()) {
            return "";
        }

        StringBuilder fieldsBuilder = new StringBuilder();
        int startFieldNumber;

        // Determine starting field number based on message type
        switch (requestType) {
            case "List" -> startFieldNumber = 3; // After limit and offset
            case "Update" -> startFieldNumber = 4; // After id, entity, and sparse
            case "Delete" -> startFieldNumber = 2; // After id
            default -> {
                return ""; // Unsupported message type
            }
        }

        // Add each qualifier as a field
        for (QualifierDescriptor qualifier : qualifiers) {
            String qualifierName = qualifier.getClassName().simpleName();
            fieldsBuilder.append("  ")
                         .append("repeated ")
                         .append(qualifierName)
                         .append(" ")
                         .append(camelCasePlural(qualifierName))
                         .append(" = ")
                         .append(startFieldNumber++)
                         .append(";\n");
        }

        return fieldsBuilder.toString();
    }

    /**
     * Converts a PascalCase class name to camelCase.
     */
    private String camelCasePlural(String className) {
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    /**
     * Gets the type mapper for a PostgreSQL type.
     */
    private PgTypeMapper getTypeMapper(String type) {
        PgTypeMapper mapper = pgTypeMappers.get(type);
        if (mapper == null) {
            throw new RuntimeException("No type mapper found for PostgreSQL type: " + type);
        }
        return mapper;
    }
}