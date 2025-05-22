package dev.logos.service.storage.pg.exporter.descriptor;

import java.util.List;

public record TableDescriptor(
    String name, 
    List<ColumnDescriptor> columns,
    List<QualifierDescriptor> qualifierDescriptors,
    List<DerivedFieldDescriptor> derivedFieldDescriptors
) implements ExportedIdentifier {
    
    public TableDescriptor(String name, List<ColumnDescriptor> columns, List<QualifierDescriptor> qualifierDescriptors) {
        this(name, columns, qualifierDescriptors, List.of());
    }
    
    @Override
    public String toString() {
        return "TableDescriptor[" +
               "name=" + name + ", " +
               "columns=" + columns + ']';
    }
}