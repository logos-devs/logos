package dev.logos.service.storage.pg.exporter.descriptor;

import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;

import java.util.Set;

import static dev.logos.service.storage.pg.Identifier.snakeToCamelCase;

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
}
