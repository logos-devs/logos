package dev.logos.service.storage.pg.exporter.descriptor;

import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;

import java.util.Set;
import java.util.regex.Pattern;

public interface ExportedIdentifier {
    Set<String> JAVA_KEYWORDS = ImmutableSet.of("abstract", "continue", "for", "new", "switch", "assert", "default",
            "goto", "package", "synchronized", "boolean", "do", "if", "private", "this", "break", "double",
            "implements", "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum",
            "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final",
            "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile", "const", "float",
            "native", "super", "while");

    String name();

    default ClassName getClassName() {
        return ClassName.bestGuess(snakeToCamelCase(name()));
    }

    default String getInstanceVariableName() {
        var className = getClassName().simpleName();
        String instanceName = className.substring(0, 1).toLowerCase() + className.substring(1);
        while (JAVA_KEYWORDS.contains(instanceName)) {
            instanceName = "_" + instanceName;
        }
        return instanceName;
    }

    // Helper method to convert snake_case to CamelCase
    static String snakeToCamelCase(String snake) {
        if (snake == null || snake.isEmpty()) {
            return "";
        }

        // Split by underscore
        String[] parts = snake.split("_");
        StringBuilder camelCase = new StringBuilder();

        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                camelCase.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    camelCase.append(part.substring(1).toLowerCase());
                }
            }
        }

        return camelCase.toString();
    }

    static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}