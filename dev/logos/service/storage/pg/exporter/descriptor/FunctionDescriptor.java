package dev.logos.service.storage.pg.exporter.descriptor;

import java.util.List;
import java.util.Optional;

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
        List<FunctionParameterDescriptor> parameters,
        /** unqualified PG type name for SETOF-composite returns, or null */
        String returnTypeName
) implements ExportedIdentifier {

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
        if (returnTypeName != null && returnType.isEmpty()) {
            throw new IllegalArgumentException("Composite return type declared but no return columns");
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
        sql.append("select * from ")
                .append(quoteIdentifier(schema))
                .append(".")
                .append(quoteIdentifier(name))
                .append("(");
        if (!parameters.isEmpty()) {
            sql.append(String.join(", ",
                    parameters.stream()
                            .map(p -> "%s => :%s".formatted(
                                    quoteIdentifier(p.name()),
                                    p.name()
                            ))
                            .toList()
            ));
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

    /**
     * For SETOF composite returns, yields e.g. Optional.of("CustomerSubscription").
     * Empty if this function does not share a composite return.
     */
    public Optional<String> protoResponseMessageName() {
        return Optional.ofNullable(returnTypeName)
                .map(rt -> {
                    String camel = snakeCaseToCamelCase(rt);
                    return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
                });
    }
}
