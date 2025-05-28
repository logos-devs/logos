package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;

public class TimestampMapper extends PgTypeMapper {
    public TimestampMapper() {
        super(List.of("pg_catalog.timestamptz", "pg_catalog.timestamp"), TYPE_STRING, "getString");
    }

    public static DateTimeFormatterBuilder getDateTimeFormatterBuilder() {
        return new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm:ss")
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .appendPattern("x");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String dbField, String protoVariable, String protoGetter) {
        return CodeBlock.builder()
                        .beginControlFlow("if (!$L.$L().isEmpty())", protoVariable, protoGetter)
                        .add("$L.bind($S, $T.parse((String) $L.$L(), new $T()" +
                                        ".appendPattern(\"yyyy-MM-dd HH:mm:ss\")" +
                                        ".appendFraction($T.MICRO_OF_SECOND, 0, 6, true)" +
                                        ".appendPattern(\"x\")" +
                                        ".toFormatter()));",
                                queryVariable, dbField, OffsetDateTime.class, protoVariable, protoGetter,
                                DateTimeFormatterBuilder.class, ChronoField.class)
                        .nextControlFlow("else")
                        .add("$L.bind($S, ($T) null);", queryVariable, dbField, OffsetDateTime.class)
                        .endControlFlow()
                        .build();
    }
}