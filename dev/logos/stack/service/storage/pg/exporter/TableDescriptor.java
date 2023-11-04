package dev.logos.stack.service.storage.exporter;

import java.util.List;

public record TableDescriptor(String name, List<ColumnDescriptor> columns) implements ExportedIdentifier {
}
