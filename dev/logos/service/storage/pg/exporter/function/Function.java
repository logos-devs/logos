package dev.logos.service.storage.pg.exporter.function;
import org.jdbi.v3.core.statement.Query;

import java.util.List;

public abstract class Function {
    private final String schema;
    private final String name;
    private final String returnType;
    private final List<FunctionParameter> parameters;

    public Function (String schema, String name, String returnType, List<FunctionParameter> parameters) {
        this.schema = schema;
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
    }

    public abstract void bind(Query query);
}
