package dev.logos.service.storage.pg.exporter;

public record ColumnDescriptor(String name, String type) implements ExportedIdentifier {

    @Override
    public String toString() {
        return "ColumnDescriptor[" +
                "name=" + name + ", " +
                "type=" + type + ']';
    }
}
