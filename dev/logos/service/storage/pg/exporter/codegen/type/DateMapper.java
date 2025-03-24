package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.time.LocalDate;
import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;

public class DateMapper extends PgTypeMapper {
    public DateMapper() {
        super(List.of("date"), TYPE_STRING, "getString");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
        return CodeBlock.of("$L.bind($S, $T.parse((String) $L.get($S)));",
                queryVariable, fieldName, LocalDate.class, fieldVariable, fieldName);
    }
}