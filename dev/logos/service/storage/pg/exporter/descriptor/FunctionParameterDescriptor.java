package dev.logos.service.storage.pg.exporter.descriptor;

import static dev.logos.service.storage.pg.exporter.descriptor.ExportedIdentifier.snakeToCamelCase;

/**
 * Descriptor for a derived field function parameter.
 */
public record FunctionParameterDescriptor(
        String name,
        String type
) implements ExportedIdentifier {

    /**
     * Creates a new parameter descriptor.
     *
     * @param name Parameter name
     * @param type PostgreSQL type name
     */
    public FunctionParameterDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Parameter name cannot be null or blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Parameter type cannot be null or blank");
        }
    }

    public String protoMethodName() {
        String protoMethodName = snakeToCamelCase(name());

        return "%s%s".formatted(
                protoMethodName.substring(0, 1).toUpperCase(),
                protoMethodName.substring(1)
        );
    }
}