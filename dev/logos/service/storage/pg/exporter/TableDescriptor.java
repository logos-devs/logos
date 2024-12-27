package dev.logos.service.storage.pg.exporter;

import java.util.List;
import java.util.Objects;

public record TableDescriptor(String name, List<ColumnDescriptor> columns) implements ExportedIdentifier {
    @Override
    public String toString() {
        return "TableDescriptor[" +
                "name=" + name + ", " +
                "columns=" + columns + ']';
    }
}