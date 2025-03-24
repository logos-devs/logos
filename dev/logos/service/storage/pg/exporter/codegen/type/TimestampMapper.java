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
        super(List.of("timestamp", "timestamptz"), TYPE_STRING, "getString");
    }

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
}