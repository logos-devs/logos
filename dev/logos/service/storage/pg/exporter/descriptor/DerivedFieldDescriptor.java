package dev.logos.service.storage.pg.exporter.descriptor;

import java.util.List;

/**
 * Descriptor for a PostgreSQL function that can be used as a derived field.
 * A derived field function must:
 * - Have its first parameter be the row type of the table
 * - Be defined in the same schema as the table
 * - Return any type (including boolean)
 * - Have optional additional parameters that can be mapped to proto types
 */
public record DerivedFieldDescriptor(
    String name,
    String returnType,
    List<DerivedFieldParameterDescriptor> parameters
) implements ExportedIdentifier {

    /**
     * Creates a new derived field descriptor.
     * 
     * @param name Function name
     * @param returnType The PostgreSQL return type of the function
     * @param parameters Parameter descriptors, excluding the row-type parameter
     */
    public DerivedFieldDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Derived field name cannot be null or blank");
        }
        if (returnType == null || returnType.isBlank()) {
            throw new IllegalArgumentException("Return type cannot be null or blank");
        }
        if (parameters == null) {
            parameters = List.of();
        }
    }
}