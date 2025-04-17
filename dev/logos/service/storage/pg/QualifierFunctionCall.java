package dev.logos.service.storage.pg;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * Represents a call to a database function that qualifies records.
 * The function must take the row type as its first parameter and return a boolean.
 */
public class QualifierFunctionCall {
    private final QualifierFunction function;
    private final Relation relation;
    private final LinkedHashMap<String, Object> parameters;

    /**
     * Creates a new qualifier function call.
     *
     * @param function   The function to call
     * @param parameters Named parameters to pass to the function (not including the row)
     */
    public QualifierFunctionCall(Relation relation, QualifierFunction function, LinkedHashMap<String, Object> parameters) {
        this.function = function;
        this.relation = relation;
        this.parameters = parameters != null ? new LinkedHashMap<>(parameters) : new LinkedHashMap<>();
    }

    /**
     * Gets the parameters to pass to the function.
     */
    public LinkedHashMap<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Gets the SQL representation of this function call.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Identifier.quoteIdentifier(relation.schema));
        sb.append(".").append(Identifier.quoteIdentifier(function.name));
        sb.append("(%s".formatted(Identifier.quoteIdentifier(relation.name))); // t is the table alias

        if (!parameters.isEmpty()) {
            for (Entry<String, Object> entry : parameters.entrySet()) {
                sb.append(", :").append(entry.getKey()).append("::").append(function.parameters.get(entry.getKey()).type());
            }
        }
        sb.append(")");

        return sb.toString();
    }
}