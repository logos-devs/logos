package dev.logos.service.storage.pg.exporter.descriptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public record SchemaDescriptor(
        String name,
        List<TableDescriptor> tables
) implements ExportedIdentifier {

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
                        """
                                SELECT
                                    column_name,
                                    CASE
                                        WHEN domain_name IS NOT NULL THEN
                                            CONCAT_WS('.', NULLIF(NULLIF(domain_schema, 'public'), 'pg_catalog'), domain_name)
                                        ELSE
                                            CONCAT_WS('.', NULLIF(NULLIF(udt_schema, 'public'), 'pg_catalog'), udt_name)
                                    END as data_type
                                FROM information_schema.columns
                                WHERE table_schema = ?
                                AND table_name = ?
                                """
                );
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

                List<QualifierDescriptor> qualifiers = extractQualifiers(connection, schema, table);
                List<DerivedFieldDescriptor> derivedFields = extractDerivedFields(connection, schema, table);
                
                tableDescriptors.add(new TableDescriptor(table, columnDescriptors, qualifiers, derivedFields));
            }

            schemaDescriptors.add(new SchemaDescriptor(
                    schemaResultSet.getString("schema_name"),
                    tableDescriptors
            ));
        }

        return schemaDescriptors;
    }

    /**
     * Extracts qualifier functions for the specified table.
     */
    private static List<QualifierDescriptor> extractQualifiers(
            Connection connection,
            String schema,
            String table) throws SQLException {

        // SQL to find qualifier functions by row type parameter
        String sql = """
                SELECT 
                    p.proname as function_name,
                    pg_catalog.pg_get_function_arguments(p.oid) as arguments
                FROM 
                    pg_catalog.pg_proc p
                    JOIN pg_catalog.pg_namespace n ON p.pronamespace = n.oid
                    JOIN pg_catalog.pg_type t ON t.typname = ?
                    JOIN pg_catalog.pg_namespace tn ON t.typnamespace = tn.oid AND tn.nspname = ?
                WHERE 
                    n.nspname = ?
                    AND pg_catalog.pg_get_function_result(p.oid) = 'boolean'
                    AND array_position(p.proargtypes, t.oid) = 0  -- First parameter is the row type
                ORDER BY 
                    p.proname;
                """;

        List<QualifierDescriptor> qualifiers = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, table);       // Table name (type name)
            stmt.setString(2, schema);      // Schema for the table type
            stmt.setString(3, schema);      // Schema for the function

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String functionName = rs.getString("function_name");
                    String arguments = rs.getString("arguments");

                    // Parse arguments to get parameter list (excluding row type)
                    List<QualifierParameterDescriptor> params = parseQualifierParameters(arguments);

                    qualifiers.add(new QualifierDescriptor(functionName, params));
                }
            }
        }

        return qualifiers;
    }

    /**
     * Extracts derived field functions for the specified table.
     * Derived field functions:
     * - Must take the table's row type as their first parameter
     * - Must be defined in the same schema as the table
     * - Can return any type (including boolean)
     */
    private static List<DerivedFieldDescriptor> extractDerivedFields(
            Connection connection,
            String schema,
            String table) throws SQLException {

        // SQL to find derived field functions by row type parameter
        String sql = """
                SELECT 
                    p.proname as function_name,
                    pg_catalog.pg_get_function_arguments(p.oid) as arguments,
                    pg_catalog.pg_get_function_result(p.oid) as return_type
                FROM 
                    pg_catalog.pg_proc p
                    JOIN pg_catalog.pg_namespace n ON p.pronamespace = n.oid
                    JOIN pg_catalog.pg_type t ON t.typname = ?
                    JOIN pg_catalog.pg_namespace tn ON t.typnamespace = tn.oid AND tn.nspname = ?
                WHERE 
                    n.nspname = ?
                    AND array_position(p.proargtypes, t.oid) = 0  -- First parameter is the row type
                ORDER BY 
                    p.proname;
                """;

        List<DerivedFieldDescriptor> derivedFields = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, table);       // Table name (type name)
            stmt.setString(2, schema);      // Schema for the table type
            stmt.setString(3, schema);      // Schema for the function (same as table schema)

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String functionName = rs.getString("function_name");
                    String arguments = rs.getString("arguments");
                    String returnType = rs.getString("return_type");

                    // Parse arguments to get parameter list (excluding row type)
                    List<DerivedFieldParameterDescriptor> params = parseDerivedFieldParameters(arguments);

                    derivedFields.add(new DerivedFieldDescriptor(functionName, returnType, params));
                }
            }
        }

        return derivedFields;
    }

    /**
     * Parses PostgreSQL function argument string into qualifier parameter descriptors.
     */
    private static List<QualifierParameterDescriptor> parseQualifierParameters(String arguments) {
        List<QualifierParameterDescriptor> result = new ArrayList<>();

        // Arguments string looks like: "row_param schema.table, param1 type1, param2 type2"
        // Split by comma first
        String[] params = arguments.split(",");

        // Skip first parameter which is the row type
        for (int i = 1; i < params.length; i++) {
            String param = params[i].trim();

            // Parameter format: "name type" possibly with default value "= something"
            // Split by whitespace but only for the first space
            int firstSpace = param.indexOf(' ');
            if (firstSpace > 0) {
                String paramName = param.substring(0, firstSpace).trim();

                // Extract type
                String paramType = param.substring(firstSpace + 1);

                // Remove default value if present
                int defaultValueIndex = paramType.indexOf('=');
                if (defaultValueIndex > 0) {
                    paramType = paramType.substring(0, defaultValueIndex).trim();
                }

                result.add(new QualifierParameterDescriptor(paramName, paramType));
            }
        }

        return result;
    }
    
    /**
     * Parses PostgreSQL function argument string into derived field parameter descriptors.
     */
    private static List<DerivedFieldParameterDescriptor> parseDerivedFieldParameters(String arguments) {
        List<DerivedFieldParameterDescriptor> result = new ArrayList<>();

        // Arguments string looks like: "row_param schema.table, param1 type1, param2 type2"
        // Split by comma first
        String[] params = arguments.split(",");

        // Skip first parameter which is the row type
        for (int i = 1; i < params.length; i++) {
            String param = params[i].trim();

            // Parameter format: "name type" possibly with default value "= something"
            // Split by whitespace but only for the first space
            int firstSpace = param.indexOf(' ');
            if (firstSpace > 0) {
                String paramName = param.substring(0, firstSpace).trim();

                // Extract type
                String paramType = param.substring(firstSpace + 1);

                // Remove default value if present
                int defaultValueIndex = paramType.indexOf('=');
                if (defaultValueIndex > 0) {
                    paramType = paramType.substring(0, defaultValueIndex).trim();
                }

                result.add(new DerivedFieldParameterDescriptor(paramName, paramType));
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return "SchemaDescriptor[" +
                "name=" + name + ", " +
                "tables=" + tables + "]";
    }
}