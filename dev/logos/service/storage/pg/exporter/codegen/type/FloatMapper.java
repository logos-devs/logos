package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT;

public class FloatMapper extends PgTypeMapper {
    public FloatMapper() {
        super(List.of("pg_catalog.float4"), TYPE_FLOAT, "getFloat");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String dbField, String protoVariable, String protoGetter) {
        return CodeBlock.of("$L.bind($S, (Float) $L.$L());",
                queryVariable, dbField, protoVariable, protoGetter);
    }
}