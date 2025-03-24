package dev.logos.service.storage.pg;

import java.util.Map;
import java.util.HashMap;

/**
 * Represents a call to a database function that qualifies records.
 * The function must take the row type as its first parameter and return a boolean.
 */
public class QualifierFunctionCall {
    private final String functionName;
    private final Map<String, Object> parameters;
    
    /**
     * Creates a new qualifier function call.
     * 
     * @param functionName Name of the function to call
     * @param parameters Named parameters to pass to the function (not including the row)
     */
    public QualifierFunctionCall(String functionName, Map<String, Object> parameters) {
        if (functionName == null || functionName.isBlank()) {
            throw new IllegalArgumentException("Function name cannot be null or blank");
        }
        
        this.functionName = functionName;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }
    
    /**
     * Gets the name of the function to call.
     */
    public String getFunctionName() {
        return functionName;
    }
    
    /**
     * Gets the parameters to pass to the function.
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    /**
     * Gets the SQL representation of this function call.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(functionName);
        sb.append("(t)"); // t is the table alias
        
        if (!parameters.isEmpty()) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                sb.append(", ");
                sb.append(":" + entry.getKey());
            }
        }
        
        return sb.toString();
    }
}