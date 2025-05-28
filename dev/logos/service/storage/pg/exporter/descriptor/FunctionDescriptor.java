package dev.logos.service.storage.pg.exporter.descriptor;

import com.squareup.javapoet.ClassName;

import java.util.List;

import static dev.logos.service.storage.pg.exporter.descriptor.ExportedIdentifier.quoteIdentifier;

/**
 * Descriptor for a PostgreSQL function that can be used as a derived field.
 * A derived field function must:
 * - Have its first parameter be the row type of the table
 * - Be defined in the same schema as the table
 * - Return any type (including boolean)
 * - Have optional additional parameters that can be mapped to proto types
 */
public record FunctionDescriptor(
        String schema,
        String name,
        List<FunctionParameterDescriptor> returnType,
        List<FunctionParameterDescriptor> parameters
) implements ExportedIdentifier {

    /**
     * Creates a new derived field descriptor.
     *
     * @param name       Function name
     * @param returnType The PostgreSQL return type of the function
     * @param parameters Parameter descriptors, excluding the row-type parameter
     */
    public FunctionDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Derived field name cannot be null or blank");
        }
        if (returnType == null || returnType.isEmpty()) {
            throw new IllegalArgumentException("Return type cannot be null or empty");
        }
        if (parameters == null) {
            parameters = List.of();
        }
    }

    static String snakeCaseToCamelCase(String snakeCase) {
        StringBuilder camelCase = new StringBuilder();
        boolean nextUpper = false;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                camelCase.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return camelCase.toString();
    }

    public String toSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quoteIdentifier(schema)).append(".").append(quoteIdentifier(name)).append("(");
        if (!parameters.isEmpty()) {
            sql.append(String.join(", ", parameters.stream()
                                                   .map(functionParameterDescriptor ->
                                                           "%s => :%s".formatted(
                                                                   quoteIdentifier(functionParameterDescriptor.name()),
                                                                   functionParameterDescriptor.name()
                                                           ))
                                                   .toList()));
        }
        sql.append(")");
        return sql.toString();
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public String rpcMethodName() {
        return snakeCaseToCamelCase(name);
    }

    public String requestMessageInstanceName() {
        return snakeCaseToCamelCase(name) + "Request";
    }

    public String requestMessageClassName() {
        return capitalize(requestMessageInstanceName());
    }

    public String responseMessageInstanceName() {
        return snakeCaseToCamelCase(name) + "Response";
    }

    public String responseMessageClassName() {
        return capitalize(responseMessageInstanceName());
    }
}