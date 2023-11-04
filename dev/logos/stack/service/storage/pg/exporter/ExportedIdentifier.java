package dev.logos.stack.service.storage.exporter;

import com.squareup.javapoet.ClassName;

import static dev.logos.stack.service.storage.pg.Identifier.snakeToCamelCase;

public interface ExportedIdentifier {
    String name();

    default ClassName getClassName() {
        return ClassName.bestGuess(snakeToCamelCase(name()));
    }

    default String getInstanceVariableName() {
        return CodeGenerator.classNameToInstanceName(getClassName().simpleName());
    }
}
