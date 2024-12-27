package dev.logos.service.storage.pg.exporter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SchemaDescriptor(String name, List<TableDescriptor> tables) implements ExportedIdentifier {

    public static List<SchemaDescriptor> extract(Connection connection, Map<String, List<String>> selectedTables) throws SQLException {
        List<SchemaDescriptor> schemaDescriptors = new ArrayList<>();

        for (String schema : selectedTables.keySet()) {
            ResultSet schemaResultSet =
                    connection.createStatement().executeQuery(
                            "select schema_name from information_schema.schemata where schema_name = '" + schema + "'");

            if (!schemaResultSet.next()) {
                throw new IllegalArgumentException("Schema " + schema + " does not exist");
            }

            List<TableDescriptor> tableDescriptors = new ArrayList<>();
            for (String table : selectedTables.get(schema)) {
                PreparedStatement tableQueryStmt = connection.prepareStatement(
                        "select table_name from information_schema.tables where table_schema = ? and table_name = ?");
                tableQueryStmt.setString(1, schema);
                tableQueryStmt.setString(2, table);
                ResultSet tableResultSet = tableQueryStmt.executeQuery();

                if (!tableResultSet.next()) {
                    throw new IllegalArgumentException("Table " + table + " does not exist");
                }

                List<ColumnDescriptor> columnDescriptors = new ArrayList<>();

                PreparedStatement columnQueryStmt = connection.prepareStatement(
                        "select column_name, udt_name::regtype::text as data_type from information_schema.columns where table_schema = ? and table_name = ?");
                columnQueryStmt.setString(1, schema);
                columnQueryStmt.setString(2, table);
                ResultSet columnResultSet = columnQueryStmt.executeQuery();

                while (columnResultSet.next()) {
                    columnDescriptors.add(
                            new ColumnDescriptor(
                                    columnResultSet.getString("column_name"),
                                    columnResultSet.getString("data_type")
                            ));
                }

                tableDescriptors.add(new TableDescriptor(table, columnDescriptors));
            }

            schemaDescriptors.add(new SchemaDescriptor(schemaResultSet.getString("schema_name"), tableDescriptors));
        }

        return schemaDescriptors;
    }

    @Override
    public String toString() {
        return "SchemaDescriptor[" +
                "name=" + name + ", " +
                "tables=" + tables + ']';
    }

}
