package dev.logos.service.storage.pg;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.logos.service.storage.pg.Identifier.quoteIdentifier;

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
    public String toQuery(HashMap<String, Object> queryParameters) {
        StringBuilder sb = new StringBuilder();
        sb.append(quoteIdentifier(relation.schema))
          .append(".")
          .append(quoteIdentifier(function.name))
          .append("(")
          .append(quoteIdentifier(relation.schema + "_" + relation.name));

        if (!parameters.isEmpty()) {
            for (Entry<String, Object> entry : parameters.entrySet()) {
                queryParameters.put(entry.getKey(), entry.getValue());
                sb.append(", ")
                  .append(":%s".formatted(entry.getKey()))
                  .append("::")
                  .append(function.parameters.get(entry.getKey()).type());
            }
        }
        sb.append(")");

        return sb.toString();
    }
}