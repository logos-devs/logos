package dev.logos.service.storage.pg.exporter.function;

public abstract class FunctionParameter {
    private final String name;
    private final String type;

    public FunctionParameter (String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
