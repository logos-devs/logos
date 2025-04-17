package dev.logos.service.storage.pg.exporter.descriptor;

import java.util.List;

public record TableDescriptor(String name, List<ColumnDescriptor> columns,
                              List<QualifierDescriptor> qualifierDescriptors) implements ExportedIdentifier {
    @Override
    public String toString() {
        return "TableDescriptor[" +
               "name=" + name + ", " +
               "columns=" + columns + ']';
    }
}