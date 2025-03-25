package dev.logos.service.storage.pg;

import java.util.HashMap;
import java.util.Map;

/**
 * Support class for QualifierFunctionCall to avoid circular dependencies.
 * This allows Select to use QualifierFunctionCall while keeping TableStorage
 * dependency structure intact.
 */
public class QualifierSupport {
    /**
     * Gets all parameter values from qualifier functions that need to be bound.
     * @param qualifierCalls List of qualifier functions
     * @return Map of parameter names to values
     */
    public static Map<String, Object> getParameters(Iterable<QualifierFunctionCall> qualifierCalls) {
        Map<String, Object> params = new HashMap<>();
        for (QualifierFunctionCall call : qualifierCalls) {
            params.putAll(call.getParameters());
        }
        return params;
    }
}