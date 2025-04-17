package dev.logos.service.storage.pg;

import java.util.LinkedHashMap;

public abstract class QualifierFunction {
    public final String name;
    public final LinkedHashMap<String, QualifierFunctionParameter> parameters;

    public QualifierFunction(
            String name,
            LinkedHashMap<String, QualifierFunctionParameter> parameters
    ) {
        this.name = name;
        this.parameters = parameters;
    }
}
