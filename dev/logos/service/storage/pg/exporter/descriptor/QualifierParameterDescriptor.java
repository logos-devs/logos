package dev.logos.service.storage.pg.exporter.descriptor;

/**
 * Descriptor for a qualifier function parameter.
 */
public record QualifierParameterDescriptor(
    String name,
    String type
) implements ExportedIdentifier {
    
    /**
     * Creates a new parameter descriptor.
     * 
     * @param name Parameter name
     * @param type PostgreSQL type name
     */
    public QualifierParameterDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Parameter name cannot be null or blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Parameter type cannot be null or blank");
        }
    }
}