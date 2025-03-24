package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT;

public class FloatMapper extends PgTypeMapper {
    public FloatMapper() {
        super(List.of("float8", "real"), TYPE_FLOAT, "getFloat");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
        return CodeBlock.of("$L.bind($S, (Float) $L.get($S));",
                queryVariable, fieldName, fieldVariable, fieldName);
    }
}