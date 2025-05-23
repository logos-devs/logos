package dev.logos.service.storage.pg;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Represents a call to a derived field function, with parameters.
 * This class helps build the SQL needed to call the function in a SELECT clause.
 */
public class DerivedFieldFunctionCall {
    private final Relation relation;
    private final DerivedFieldFunction function;
    private final LinkedHashMap<String, Object> parameters;

    /**
     * Creates a new derived field function call.
     * 
     * @param relation the table or relation this function is applied to
     * @param function the derived field function to call
     * @param parameters named parameters to pass to the function
     */
    public DerivedFieldFunctionCall(Relation relation, DerivedFieldFunction function, LinkedHashMap<String, Object> parameters) {
        this.relation = relation;
        this.function = function;
        this.parameters = parameters;
    }

    /**
     * Generates the SQL expression to call this derived field function in a SELECT clause.
     * 
     * @param parameters map where query parameters will be added
     * @param index a counter to generate unique parameter names
     * @return SQL expression for the function call
     */
    public String toSelectExpression(Map<String, Object> parameters, AtomicInteger index) {
        String tableAlias = relation.schema + "_" + relation.name;
        
        String paramsList = this.parameters.isEmpty() ? "" : ", " + 
            this.parameters.entrySet().stream()
                .map(entry -> {
                    String paramName = "p_" + index.getAndIncrement();
                    parameters.put(paramName, entry.getValue());
                    return "$" + paramName;
                })
                .collect(Collectors.joining(", "));
                
        return String.format("%s.%s(%s%s) as \"%s\"",
                             relation.schema,
                             function.name,
                             Identifier.quoteIdentifier(tableAlias),
                             paramsList,
                             function.name);
    }

    /**
     * Gets the derived field function associated with this call.
     */
    public DerivedFieldFunction getFunction() {
        return function;
    }

    /**
     * Gets the parameters for this function call.
     */
    public LinkedHashMap<String, Object> getParameters() {
        return parameters;
    }
}