package dev.logos.service.storage.pg;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for processing qualifier objects in storage service requests.
 * This is used to extract qualifier parameters from generated protocol buffer objects
 * and add them to the SQL query.
 */
public class StorageServiceQualifierExtension {
    private static final Logger logger = Logger.getLogger(StorageServiceQualifierExtension.class.getName());
    
    /**
     * Adds qualifier function calls to a Select builder from a request object.
     * 
     * @param builder Select builder to add qualifiers to
     * @param requestType Type of request (List, Update, Delete)
     * @param request Request object that may contain qualifiers
     */
    public static void addQualifiers(Select.Builder builder, String requestType, Object request) {
        if (request == null) {
            return;
        }
        
        try {
            // Try to find a get[RequestType]QualifiersList method - this would be generated
            // in the proto class if qualifiers exist for this request type
            Method getQualifiersMethod = request.getClass().getMethod(
                "get" + requestType + "QualifiersList");
                
            List<?> qualifiers = (List<?>) getQualifiersMethod.invoke(request);
            if (qualifiers == null || qualifiers.isEmpty()) {
                return;
            }
            
            // Process each qualifier object
            for (Object qualifier : qualifiers) {
                processQualifier(builder, qualifier);
            }
        } catch (NoSuchMethodException e) {
            // No qualifiers for this request type, that's fine
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to process qualifiers", e);
        }
    }
    
    /**
     * Processes a single qualifier object and adds it to the builder.
     */
    private static void processQualifier(Select.Builder builder, Object qualifier) {
        if (qualifier == null) {
            return;
        }
        
        // Extract qualifier name from class name
        String qualifierName = qualifier.getClass().getSimpleName();
        
        // Extract parameters from the qualifier object
        Map<String, Object> params = extractParameters(qualifier);
        
        // Add the qualifier to the builder
        builder.qualifier(qualifierName, params);
    }
    
    /**
     * Extracts parameters from a qualifier object using its getter methods.
     */
    private static Map<String, Object> extractParameters(Object qualifier) {
        Map<String, Object> params = new HashMap<>();
        
        try {
            // Get all methods from the qualifier class
            Method[] methods = qualifier.getClass().getMethods();
            
            for (Method method : methods) {
                String methodName = method.getName();
                
                // Look for getter methods
                if (methodName.startsWith("get") && 
                    !methodName.equals("getClass") &&
                    !methodName.equals("getDefaultInstanceForType") &&
                    !methodName.endsWith("Value") && // Skip *Value accessors for enums
                    method.getParameterCount() == 0) {
                    
                    // Convert method name to parameter name (getParamName -> paramName)
                    String paramName = methodName.substring(3);
                    paramName = Character.toLowerCase(paramName.charAt(0)) + paramName.substring(1);
                    
                    // Get the parameter value
                    Object value = method.invoke(qualifier);
                    if (value != null) {
                        params.put(paramName, value);
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to extract qualifier parameters", e);
        }
        
        return params;
    }
}