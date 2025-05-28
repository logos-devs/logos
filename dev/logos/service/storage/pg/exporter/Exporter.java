package dev.logos.service.storage.pg.exporter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.squareup.javapoet.JavaFile;
import dev.logos.module.ModuleLoader;
import dev.logos.service.storage.pg.exporter.codegen.proto.ProtoGenerator;
import dev.logos.service.storage.pg.exporter.codegen.service.StorageServiceBaseGenerator;
import dev.logos.service.storage.pg.exporter.descriptor.FunctionDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.FunctionParameterDescriptor;
import dev.logos.service.storage.pg.exporter.module.ExportModule;
import dev.logos.service.storage.pg.exporter.module.annotation.BuildDir;
import dev.logos.service.storage.pg.exporter.module.annotation.BuildPackage;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;


public class Exporter {
    private final Path buildDir;
    private final String buildPackage;
    private final DataSource dataSource;
    private final ProtoGenerator protoGenerator;
    private final StorageServiceBaseGenerator storageServiceBaseGenerator;
    private final Gson gson = new GsonBuilder().create();

    @Inject
    public Exporter(
            @BuildDir String buildDir,
            @BuildPackage String buildPackage,
            DataSource dataSource,
            ProtoGenerator protoGenerator,
            StorageServiceBaseGenerator storageServiceBaseGenerator
    ) {
        this.buildDir = Path.of(buildDir);
        this.buildPackage = buildPackage;
        this.dataSource = dataSource;
        this.protoGenerator = protoGenerator;
        this.storageServiceBaseGenerator = storageServiceBaseGenerator;
    }

    private static Map<Integer, String> getQualifiedTypeNames(Connection connection, Set<Integer> oids) throws SQLException {
        if (oids.isEmpty()) return Collections.emptyMap();
        String inSql = oids.stream().map(String::valueOf).collect(Collectors.joining(","));
        String sql = """
                SELECT t.oid, pn.nspname || '.' || t.typname AS qname
                FROM pg_type t
                JOIN pg_namespace pn ON t.typnamespace = pn.oid
                WHERE t.oid IN (%s)
                """.formatted(inSql);
        Map<Integer, String> out = new HashMap<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.put(rs.getInt(1), rs.getString(2));
        }
        return out;
    }

    /*
     * Lookup functions in the database based on the selected functions.
     * This method will return a list of FunctionDescriptor objects that represent the functions found in the database.
     *
     * The input JSON will map a service class name to be generated to a list of function signatures.
     *
     * Example input JSON:
     * {
     *     "MessageStorageServiceBase": [
     *         "author.message_list(integer, integer)",
     *         "author.message_update(uuid, integer)",
     *         "author.message_delete(uuid, integer)",
     *         "author.message_create(integer, integer)",
     *     ]
     * }
     */
    public static Map<String, List<FunctionDescriptor>> lookupFunctions(
            Connection connection,
            Map<String, List<String>> selectedFunctions) throws SQLException {

        Map<String, List<FunctionDescriptor>> serviceMap = new HashMap<>();
        Map<Integer, String> typeOidToQualifiedName = new HashMap<>(); // Cache for qualified type names

        for (var entry : selectedFunctions.entrySet()) {
            String serviceClassName = entry.getKey();
            List<FunctionDescriptor> descriptors = new ArrayList<>();

            for (String signature : entry.getValue()) {
                // --- 1. Lookup function metadata by regprocedure OID ---
                String metaSql = """
                        SELECT
                          p.oid,
                          n.nspname AS schema,
                          p.proname AS name,
                          p.proargnames,
                          p.proargtypes,
                          p.prorettype,
                          t.typtype AS rettypekind,
                          t.oid AS rettypeoid
                        FROM pg_proc p
                        JOIN pg_namespace n ON p.pronamespace = n.oid
                        JOIN pg_type t ON p.prorettype = t.oid
                        WHERE p.oid = ?::regprocedure
                        """;
                long rettypeOid;
                String schema, funcName, rettypeKind;
                String[] argNames;
                int[] argOids;

                try (PreparedStatement stmt = connection.prepareStatement(metaSql)) {
                    stmt.setString(1, signature);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next())
                            continue; // Function signature did not resolve, skip

                        schema = rs.getString("schema");
                        funcName = rs.getString("name");
                        rettypeOid = rs.getLong("rettypeoid");
                        rettypeKind = rs.getString("rettypekind");

                        // Parse proargtypes (oidvector: "23 20 1114")
                        String proargtypesStr = rs.getString("proargtypes");
                        argOids = (proargtypesStr != null && !proargtypesStr.isBlank())
                                ? Arrays.stream(proargtypesStr.trim().split("\\s+"))
                                        .filter(s -> !s.isEmpty())
                                        .mapToInt(Integer::parseInt)
                                        .toArray()
                                : new int[0];

                        // Get proargnames (String[], may be null or shorter than argOids)
                        Array namesArr = rs.getArray("proargnames");
                        argNames = (namesArr != null) ? (String[]) namesArr.getArray() : new String[0];

                        // Strict: Only include if all param names are present and non-blank
                        if (argNames.length < argOids.length ||
                                Arrays.stream(argNames).anyMatch(n -> n == null || n.isBlank())) {
                            continue; // Skip this function, strict param name policy
                        }
                    }
                }

                // --- 2. Gather all OIDs for type name lookup (parameters + return + composite fields) ---
                Set<Integer> allTypeOids = new HashSet<>();
                Arrays.stream(argOids).forEach(allTypeOids::add);
                allTypeOids.add((int) rettypeOid);

                // --- 3. If return is composite, get its field info ---
                List<FunctionParameterDescriptor> returnType;
                List<String> returnFieldNames = new ArrayList<>();
                List<Integer> returnFieldOids = new ArrayList<>();
                if ("c".equals(rettypeKind)) {
                    // Decompose composite fields
                    try (PreparedStatement stmt = connection.prepareStatement("""
                            SELECT a.attname, a.atttypid
                              FROM pg_attribute a
                             WHERE a.attrelid = (SELECT typrelid FROM pg_type WHERE oid = ?)
                               AND a.attnum > 0 AND NOT a.attisdropped
                             ORDER BY a.attnum
                            """)) {
                        stmt.setLong(1, rettypeOid);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                returnFieldNames.add(rs.getString(1));
                                int foid = rs.getInt(2);
                                returnFieldOids.add(foid);
                                allTypeOids.add(foid);
                            }
                        }
                    }
                }

                // --- 4. Batch fetch qualified type names for OIDs not yet cached ---
                Set<Integer> missing = allTypeOids.stream()
                                                  .filter(oid -> !typeOidToQualifiedName.containsKey(oid))
                                                  .collect(Collectors.toSet());
                if (!missing.isEmpty()) {
                    String oidsIn = missing.stream().map(String::valueOf).collect(Collectors.joining(","));
                    String typeSql = """
                            SELECT t.oid, n.nspname, t.typname
                              FROM pg_type t JOIN pg_namespace n ON t.typnamespace = n.oid
                             WHERE t.oid IN (%s)
                            """.formatted(oidsIn);
                    try (Statement stmt = connection.createStatement();
                         ResultSet rs = stmt.executeQuery(typeSql)) {
                        while (rs.next()) {
                            int oid = rs.getInt(1);
                            String typSchema = rs.getString(2);
                            String typName = rs.getString(3);
                            // Always fully qualify, even pg_catalog/public
                            typeOidToQualifiedName.put(oid, typSchema + "." + typName);
                        }
                    }
                }

                // --- 5. Build FunctionParameterDescriptor for parameters (using strictly argNames, argOids) ---
                List<FunctionParameterDescriptor> params = IntStream.range(0, argOids.length)
                                                                    .mapToObj(i -> new FunctionParameterDescriptor(
                                                                            argNames[i],
                                                                            typeOidToQualifiedName.getOrDefault(argOids[i], "unknown")
                                                                    ))
                                                                    .collect(Collectors.toList());

                // --- 6. Build FunctionParameterDescriptor for return type ---
                if (!returnFieldOids.isEmpty()) {
                    // Composite return type
                    returnType = IntStream.range(0, returnFieldOids.size())
                                          .mapToObj(i -> new FunctionParameterDescriptor(
                                                  returnFieldNames.get(i),
                                                  typeOidToQualifiedName.getOrDefault(returnFieldOids.get(i), "unknown"))
                                          )
                                          .collect(Collectors.toList());
                } else {
                    // Scalar/domain return type
                    returnType = List.of(
                            new FunctionParameterDescriptor("result",
                                    typeOidToQualifiedName.getOrDefault((int) rettypeOid, "unknown"))
                    );
                }

                // --- 7. Assemble and add FunctionDescriptor ---
                descriptors.add(new FunctionDescriptor(schema, funcName, returnType, params));
            }
            if (!descriptors.isEmpty())
                serviceMap.put(serviceClassName, descriptors);
        }
        return serviceMap;
    }

    public static void main(String[] args) throws SQLException, IOException {
        ExportType exportType = ExportType.valueOf(args[1].toUpperCase());
        String buildDir = args[2];
        String buildPackage = args[3];
        String targetJson = args[4]; // a Map<String, List<String>> in JSON format

        Injector injector = ModuleLoader.createInjector(new ExportModule(buildDir, buildPackage));
        Exporter exporter = injector.getInstance(Exporter.class);

        switch (exportType) {
            case JSON:
                exporter.exportJson(targetJson);
                return;
            case PROTO:
                exporter.generateProto(targetJson);
                return;
            case JAVA:
                exporter.generateJava(targetJson);
        }
    }

    public void exportJson(String selectedFunctionsJson) throws SQLException, IOException {
        // {"MessageStorageServiceBase":["author.message_list(integer, integer)","author.message_update(uuid, integer)","author.message_delete(uuid, integer)","author.message_create(integer, integer)"]}
        try (Connection connection = dataSource.getConnection()) {
            Map<String, List<FunctionDescriptor>> functionDescriptors = lookupFunctions(
                    connection,
                    new Gson().fromJson(selectedFunctionsJson, new TypeToken<Map<String, List<String>>>() {
                    }.getType()));

            try (OutputStream outputStream = Files.newOutputStream(
                    Files.createDirectories(
                                 Path.of("%s/%s/".formatted(buildDir, buildPackage.replace(".", "/"))))
                         .resolve("schema_export.json"),
                    CREATE, WRITE)) {

                outputStream.write(gson.toJson(functionDescriptors).getBytes());
            }
        }
    }

    public Map<String, List<FunctionDescriptor>> loadDescriptorsFromJson(String json) {
        System.err.println("Loading function descriptors from JSON: " + json);

        Map<String, List<FunctionDescriptor>> descriptors = gson.fromJson(json, new TypeToken<Map<String, List<FunctionDescriptor>>>() {
        }.getType());

        System.err.println(descriptors);

        return descriptors;
    }

    public void generateJava(String selectedFunctionsJson) throws IOException {
        Map<String, List<FunctionDescriptor>> functionDescriptors = loadDescriptorsFromJson(selectedFunctionsJson);

        for (Map.Entry<String, List<FunctionDescriptor>> entry : functionDescriptors.entrySet()) {
            String serviceName = entry.getKey();
            List<FunctionDescriptor> serviceFunctions = entry.getValue();
            System.err.println("buildDir" + " = " + buildDir);
            System.err.println("buildPackage" + " = " + buildPackage);
            System.err.println("serviceName" + " = " + serviceName);
            System.err.println("serviceFunctions" + " = " + serviceFunctions);

            JavaFile.builder(buildPackage, storageServiceBaseGenerator.generate(buildPackage, serviceName, serviceFunctions))
                    .build()
                    .writeToPath(buildDir);
        }
    }

    public void generateProto(String selectedFunctionsJson) throws IOException {
        Map<String, List<FunctionDescriptor>> functionDescriptors = loadDescriptorsFromJson(selectedFunctionsJson);

        for (Map.Entry<String, List<FunctionDescriptor>> entry : functionDescriptors.entrySet()) {
            String serviceName = entry.getKey();
            List<FunctionDescriptor> serviceFunctions = entry.getValue();

            Path protoPath = Files.createDirectories(
                    Path.of("%s/%s/".formatted(
                            buildDir,
                            buildPackage.replace(".", "/")
                    ))
            ).resolve("%s.proto".formatted(serviceName));
            String protoContent = protoGenerator.generate(buildPackage, serviceName, serviceFunctions);

            try (OutputStream outputStream = Files.newOutputStream(protoPath, CREATE, WRITE)) {
                outputStream.write(protoContent.getBytes());
            }
        }
    }

    private enum ExportType {
        JSON,
        PROTO,
        JAVA
    }
}
