package dev.logos.service.storage.pg.exporter;

import com.squareup.javapoet.ClassName;

import static dev.logos.service.storage.pg.Identifier.snakeToCamelCase;

public interface ExportedIdentifier {
    String name();

    default ClassName getClassName() {
        return ClassName.bestGuess(snakeToCamelCase(name()));
    }

    default String getInstanceVariableName() {
        return CodeGenerator.classNameToInstanceName(getClassName().simpleName());
    }
}
