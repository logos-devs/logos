package dev.logos.service.storage.pg.exporter;

import java.util.List;
import java.util.Objects;

public final class TableDescriptor implements ExportedIdentifier {
    private final String name;
    private final List<ColumnDescriptor> columns;

    public TableDescriptor(String name, List<ColumnDescriptor> columns) {
        this.name = name;
        this.columns = columns;
    }

    @Override
    public String name() {
        return name;
    }

    public List<ColumnDescriptor> columns() {
        return columns;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TableDescriptor) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.columns, that.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columns);
    }

    @Override
    public String toString() {
        return "TableDescriptor[" +
                "name=" + name + ", " +
                "columns=" + columns + ']';
    }

}
