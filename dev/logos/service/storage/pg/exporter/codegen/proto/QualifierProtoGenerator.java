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

            fieldsBuilder.append("    ")
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
     * With the QualifierCall pattern, we no longer add qualifier fields directly to request messages.
     * Instead, we use the qualifier_call field that's added in ProtoGenerator.protoListRequestMessage.
     *
     * @param requestType Type of request (List, Update, Delete)
     * @param qualifiers  List of qualifier descriptors
     * @return Empty string as we no longer generate direct qualifier fields
     */
    public String generateQualifierFields(String requestType, List<QualifierDescriptor> qualifiers) {
        // We're now using QualifierCall approach, so don't generate direct fields
        return "";
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