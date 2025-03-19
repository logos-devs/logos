package dev.logos.service.storage.pg.exporter.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.protobuf.ByteString;
import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.Converter;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;
import dev.logos.service.storage.pg.exporter.module.annotation.BuildDir;
import dev.logos.service.storage.pg.exporter.module.annotation.BuildPackage;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.*;

public class ExportModule extends AbstractModule {
    private final String buildDir;
    private final String buildPackage;

    public ExportModule(String buildDir, String buildPackage) {
        this.buildDir = buildDir;
        this.buildPackage = buildPackage;
    }

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(BuildDir.class).toInstance(buildDir);
        bind(String.class).annotatedWith(BuildPackage.class).toInstance(buildPackage);

        MapBinder<String, PgTypeMapper> pgTypeMapperBinder =
                MapBinder.newMapBinder(binder(), String.class, PgTypeMapper.class);

        List.of(
                new PgTypeMapper(List.of("smallint", "integer"), TYPE_SINT32, "getInt") {
                    @Override
                    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
                        return CodeBlock.of("$L.bind($S, (Integer) $L.get($S));",
                                            queryVariable, fieldName, fieldVariable, fieldName);
                    }
                },
                new PgTypeMapper(List.of("bigint"), TYPE_SINT64, "getLong") {
                    @Override
                    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
                        return CodeBlock.of("$L.bind($S, (BigInteger) $L.get($S));",
                                            queryVariable, fieldName, fieldVariable, fieldName);
                    }
                },
                new PgTypeMapper(List.of("float8", "real"), TYPE_FLOAT, "getFloat") {
                    @Override
                    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
                        return CodeBlock.of("$L.bind($S, (Float) $L.get($S));",
                                            queryVariable, fieldName, fieldVariable, fieldName);
                    }
                },
                new PgTypeMapper(List.of("double precision"), TYPE_DOUBLE, "getDouble") {
                    @Override
                    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
                        return CodeBlock.of("$L.bind($S, (Double) $L.get($S));",
                                            queryVariable, fieldName, fieldVariable, fieldName);
                    }
                },
                new PgTypeMapper(List.of("numeric", "decimal"), TYPE_FIXED64, "getBigDecimal") {
                    @Override
                    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
                        return CodeBlock.of("$L.bind($S, (BigDecimal) $L.get($S));",
                                            queryVariable, fieldName, fieldVariable, fieldName);
                    }
                },
                new PgTypeMapper(List.of("char", "varchar", "character varying", "text"), TYPE_STRING, "getString") {
                    @Override
                    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
                        return CodeBlock.of("$L.bind($S, (String) $L.get($S));",
                                            queryVariable, fieldName, fieldVariable, fieldName);
                    }
                },
                new PgTypeMapper(List.of("timestamp", "timestamptz"), TYPE_STRING, "getString") {
                    @Override
                    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
                        return CodeBlock.of("$L.bind($S, $T.parse((String) $L.get($S), new $T()" +
                                                    ".appendPattern(\"yyyy-MM-dd HH:mm:ss\")" +
                                                    ".appendFraction($T.MICRO_OF_SECOND, 0, 6, true)" +
                                                    ".appendPattern(\"x\")" +
                                                    ".toFormatter()));",
                                            queryVariable, fieldName, OffsetDateTime.class, fieldVariable, fieldName,
                                            DateTimeFormatterBuilder.class, ChronoField.class);
                    }
                },
                new PgTypeMapper(List.of("date"), TYPE_STRING, "getString") {
                    @Override
                    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
                        return CodeBlock.of("$L.bind($S, LocalDate.parse((String) $L.get($S)));",
                                            queryVariable, fieldName, fieldVariable, fieldName);
                    }
                },
                new PgTypeMapper(List.of("bool"), TYPE_BOOL, "getBoolean") {
                    @Override
                    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
                        return CodeBlock.of("$L.bind($S, (Boolean) $L.get($S));",
                                            queryVariable, fieldName, fieldVariable, fieldName);
                    }
                },
                new PgTypeMapper(List.of("uuid"), TYPE_BYTES, "getObject") {
                    @Override
                    public String resultSetFieldCast() {
                        return "(java.util.UUID)";
                    }

                    @Override
                    public CodeBlock pgToProto(CodeBlock innerCall) {
                        return CodeBlock.of("$T.uuidToBytestring($L)", Converter.class, innerCall);
                    }

                    @Override
                    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
                        return CodeBlock.of("$L.bind($S, $T.bytestringToUuid(($T) $L.get($S)));",
                                            queryVariable, fieldName, Converter.class, ByteString.class, fieldVariable, fieldName);
                    }
                },
                new PgTypeMapper(List.of("bytea"), TYPE_BYTES, "getBytes") {
                    @Override
                    public CodeBlock pgToProto(CodeBlock innerCall) {
                        return CodeBlock.of("$T.copyFrom($L)", ByteString.class, innerCall);
                    }

                    @Override
                    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
                        return CodeBlock.of("$L.bind($S, (($T) $L.get($S)).toByteArray());",
                                            queryVariable, fieldName, ByteString.class, fieldVariable, fieldName);
                    }
                }
        ).forEach((PgTypeMapper typeMapper) -> typeMapper.getPgTypes().forEach(
                (String pgType) -> pgTypeMapperBinder.addBinding(pgType).toInstance(typeMapper)
        ));
    }
}
/*
    return switch (this.type) {
        case "text[]" -> "(String[])";
        default -> "";
    };

// protoToPg
    case "text[]" ->
            CodeBlock.of("$L.bind($S, (String[]) $L.get($S));\n", queryVariable, fieldName, fieldVariable, fieldName);

    public String resultSetFieldGetter() {
        if (isArray()) {
            return "getArray";
        } else {
            return switch (this.type) {
                case "text[]" -> "getString";
                default -> throw new IllegalArgumentException("Unsupported type: " + this.type);
            };
        }
    }


     from db to protobuf type
CodeBlock protobufTypeConverter(CodeBlock innerCall) {
    if (isArray()) {
        return CodeBlock.of("$T.asList((String[])$L.getArray())", Arrays.class, innerCall);
    } else {
        return switch (type) {
            // other converters
            default -> innerCall;
        };
    }
}


*/