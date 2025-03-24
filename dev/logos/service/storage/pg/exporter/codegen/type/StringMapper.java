package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;

public class StringMapper extends PgTypeMapper {
    public StringMapper() {
        super(List.of("char", "varchar", "character varying", "text"), TYPE_STRING, "getString");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
        return CodeBlock.of("$L.bind($S, (String) $L.get($S));",
                queryVariable, fieldName, fieldVariable, fieldName);
    }
}