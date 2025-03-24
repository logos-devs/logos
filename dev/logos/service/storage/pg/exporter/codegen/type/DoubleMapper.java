package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE;

public class DoubleMapper extends PgTypeMapper {
    public DoubleMapper() {
        super(List.of("double precision"), TYPE_DOUBLE, "getDouble");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
        return CodeBlock.of("$L.bind($S, (Double) $L.get($S));",
                queryVariable, fieldName, fieldVariable, fieldName);
    }
}