package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32;

public class IntegerMapper extends PgTypeMapper {
    public IntegerMapper() {
        super(List.of("smallint", "integer"), TYPE_SINT32, "getInt");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
        return CodeBlock.of("$L.bind($S, (Integer) $L.get($S));",
                queryVariable, fieldName, fieldVariable, fieldName);
    }
}