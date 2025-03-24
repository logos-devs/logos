package dev.logos.service.storage.pg.exporter.descriptor;

import java.util.List;

/**
 * Descriptor for a PostgreSQL function that can be used as a qualifier.
 * A qualifier function must:
 * - Return a boolean value
 * - Have its first parameter be the row type of the table
 * - Have additional parameters that can be mapped to proto types
 */
public record QualifierDescriptor(
    String name,
    List<QualifierParameterDescriptor> parameters
) implements ExportedIdentifier {

    /**
     * Creates a new qualifier descriptor.
     * 
     * @param name Function name
     * @param parameters Parameter descriptors, excluding the row-type parameter
     */
    public QualifierDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Qualifier name cannot be null or blank");
        }
        if (parameters == null) {
            parameters = List.of();
        }
    }
}