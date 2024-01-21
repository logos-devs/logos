package dev.logos.service.storage.pg.exporter;

import java.util.List;

public record TableDescriptor(String name, List<ColumnDescriptor> columns) implements ExportedIdentifier {
}