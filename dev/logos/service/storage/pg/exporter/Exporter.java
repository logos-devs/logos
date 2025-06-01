package dev.logos.service.storage.pg.exporter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;


public class Exporter {
    private static final Logger logger = LoggerFactory.getLogger(Exporter.class);

    private final Path buildDir;
    private final String buildPackage;
    private final DataSource dataSource;
    private final ProtoGenerator protoGenerator;
    private final StorageServiceBaseGenerator storageServiceBaseGenerator;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
            Map<String, List<String>> selectedFunctions
    ) throws SQLException {

        Map<Integer, String> typeCache = new HashMap<>();
        Map<String, List<FunctionDescriptor>> result = new HashMap<>();

        String metaSql = """
                select
                  p.oid,
                  n.nspname       as schema,
                  p.proname       as name,
                  p.proargtypes,            -- input-only types
                  p.proargnames,            -- names for IN + OUT
                  p.proallargtypes,         -- all types (IN + OUT)
                  p.proargmodes,            -- modes array parallel to proallargtypes
                  p.prorettype,
                  p.proretset,
                  t.typtype       as rettypekind,
                  t.oid           as rettypeoid
                from pg_proc p
                join pg_namespace n on p.pronamespace = n.oid
                join pg_type t      on p.prorettype  = t.oid
                where p.oid = ?::regprocedure
                """;

        for (var svcEntry : selectedFunctions.entrySet()) {
            String serviceName = svcEntry.getKey();
            List<FunctionDescriptor> descriptors = new ArrayList<>();

            for (String signature : svcEntry.getValue()) {
                try (PreparedStatement ps = connection.prepareStatement(metaSql)) {
                    ps.setString(1, signature);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Cannot export function " + signature + ": function not found"
                            );
                        }

                        String schema = rs.getString("schema");
                        String funcName = rs.getString("name");
                        String inOidsStr = rs.getString("proargtypes");
                        Array inNamesArr = rs.getArray("proargnames");
                        Array allOidsArr = rs.getArray("proallargtypes");
                        Array modesArr = rs.getArray("proargmodes");
                        long rettypeOid = rs.getLong("rettypeoid");
                        boolean isSetOf = rs.getBoolean("proretset");
                        String rettypeKind = rs.getString("rettypekind");

                        // new: holder for unqualified composite returnTypeName
                        String returnTypeName = null;

                        // parse IN-params
                        int[] inOids = (inOidsStr == null || inOidsStr.isBlank())
                                ? new int[0]
                                : Arrays.stream(inOidsStr.trim().split("\\s+"))
                                .mapToInt(Integer::parseInt)
                                .toArray();
                        String[] inNames = inNamesArr != null
                                ? (String[]) inNamesArr.getArray()
                                : new String[0];

                        // raw holders
                        class Param {
                            final String name;
                            final int oid;

                            Param(String n, int o) {
                                name = n;
                                oid = o;
                            }
                        }
                        List<Param> inParamsRaw = new ArrayList<>();
                        List<Param> returnRaw = new ArrayList<>();
                        Set<Integer> allTypeOids = new HashSet<>();

                        Arrays.stream(inOids).forEach(allTypeOids::add);
                        allTypeOids.add((int) rettypeOid);

                        // CASE A: RETURNS TABLE / OUT / INOUT
                        if (allOidsArr != null && modesArr != null) {
                            Long[] allOids = (Long[]) allOidsArr.getArray();
                            String[] modes = (String[]) modesArr.getArray();
                            String[] allNames = inNamesArr != null
                                    ? (String[]) inNamesArr.getArray()
                                    : new String[0];

                            for (int i = 0; i < modes.length; i++) {
                                int oid = allOids[i].intValue();
                                String mode = modes[i];
                                String name = i < allNames.length ? allNames[i] : null;
                                if (name == null || name.isBlank()) {
                                    throw new IllegalArgumentException(
                                            "Cannot export function " + signature +
                                                    ": missing name for " + mode + "-parameter at position " + i
                                    );
                                }
                                allTypeOids.add(oid);
                                switch (mode) {
                                    case "i" -> inParamsRaw.add(new Param(name, oid));
                                    case "o", "t" -> returnRaw.add(new Param(name, oid));
                                    case "b" -> {
                                        inParamsRaw.add(new Param(name, oid));
                                        returnRaw.add(new Param(name, oid));
                                    }
                                    default -> { /* ignore variadic etc. */ }
                                }
                            }
                            if (returnRaw.isEmpty()) {
                                throw new IllegalArgumentException(
                                        "Cannot export function " + signature +
                                                ": declared RETURNS TABLE but found no output columns"
                                );
                            }
                        }
                        // CASE B: RETURNS SETOF composite_type
                        else if (isSetOf && "c".equals(rettypeKind)) {
                            // IN params
                            for (int i = 0; i < inOids.length; i++) {
                                if (i >= inNames.length || inNames[i] == null || inNames[i].isBlank()) {
                                    throw new IllegalArgumentException(
                                            "Cannot export function " + signature +
                                                    ": missing name for IN parameter at position " + i
                                    );
                                }
                                inParamsRaw.add(new Param(inNames[i], inOids[i]));
                            }
                            // decompose composite
                            try (PreparedStatement ds = connection.prepareStatement("""
                                    select attname, atttypid
                                      from pg_attribute
                                     where attrelid = (
                                           select typrelid
                                             from pg_type
                                            where oid = ?
                                         )
                                       and attnum > 0
                                       and not attisdropped
                                     order by attnum
                                    """)) {
                                ds.setLong(1, rettypeOid);
                                try (ResultSet dr = ds.executeQuery()) {
                                    while (dr.next()) {
                                        returnRaw.add(new Param(dr.getString(1), dr.getInt(2)));
                                        allTypeOids.add(dr.getInt(2));
                                    }
                                }
                            }
                            if (returnRaw.isEmpty()) {
                                throw new IllegalArgumentException(
                                        "Cannot export function " + signature +
                                                ": RETURNS SETOF composite but composite has no attributes"
                                );
                            }
                        }
                        // unsupported
                        else {
                            throw new IllegalArgumentException(
                                    "Cannot export function " + signature +
                                            ": only RETURNS TABLE(...) or RETURNS SETOF composite_type are supported"
                            );
                        }

                        // batch-fetch type names
                        Set<Integer> missing = allTypeOids.stream()
                                .filter(o -> !typeCache.containsKey(o))
                                .collect(Collectors.toSet());
                        if (!missing.isEmpty()) {
                            String inList = missing.stream().map(String::valueOf).collect(Collectors.joining(","));
                            String typeSql = String.format("""
                                    SELECT t.oid, n.nspname, t.typname
                                      FROM pg_type t
                                      JOIN pg_namespace n ON t.typnamespace = n.oid
                                     WHERE t.oid IN (%s)
                                    """, inList);
                            try (Statement ts = connection.createStatement();
                                 ResultSet tr = ts.executeQuery(typeSql)) {
                                while (tr.next()) {
                                    typeCache.put(tr.getInt(1),
                                            tr.getString(2) + "." + tr.getString(3));
                                }
                            }
                        }

                        // capture unqualified composite name if SETOF
                        if (isSetOf && "c".equals(rettypeKind)) {
                            String fq = typeCache.get((int) rettypeOid);
                            returnTypeName = fq.contains(".")
                                    ? fq.substring(fq.indexOf('.') + 1)
                                    : fq;
                        }

                        // materialize descriptors
                        List<FunctionParameterDescriptor> inParams = inParamsRaw.stream()
                                .map(p -> new FunctionParameterDescriptor(p.name, typeCache.get(p.oid)))
                                .collect(Collectors.toList());
                        List<FunctionParameterDescriptor> outParams = returnRaw.stream()
                                .map(p -> new FunctionParameterDescriptor(p.name, typeCache.get(p.oid)))
                                .collect(Collectors.toList());

                        descriptors.add(new FunctionDescriptor(
                                schema,
                                funcName,
                                outParams,
                                inParams,
                                returnTypeName
                        ));
                    }
                }
            }

            if (!descriptors.isEmpty()) {
                result.put(serviceName, descriptors);
            }
        }

        return result;
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
        logger.atInfo()
              .addKeyValue("json", json)
              .log("Loading function descriptors from JSON");

        JsonElement jsonElement = JsonParser.parseString(json);
        Map<String, List<FunctionDescriptor>> descriptors = gson.fromJson(jsonElement, new TypeToken<Map<String, List<FunctionDescriptor>>>() {
        }.getType());

        logger.atInfo().log(gson.toJson(jsonElement));

        return descriptors;
    }

    public void generateJava(String selectedFunctionsJson) throws IOException {
        Map<String, List<FunctionDescriptor>> functionDescriptors = loadDescriptorsFromJson(selectedFunctionsJson);

        for (Map.Entry<String, List<FunctionDescriptor>> entry : functionDescriptors.entrySet()) {
            String serviceName = entry.getKey();
            List<FunctionDescriptor> serviceFunctions = entry.getValue();
            logger.atInfo()
                  .addKeyValue("buildDir", buildDir)
                  .addKeyValue("buildPackage", buildPackage)
                  .addKeyValue("serviceName", serviceName)
                  .addKeyValue("serviceFunctions", serviceFunctions)
                  .log("Generating Java service");

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
