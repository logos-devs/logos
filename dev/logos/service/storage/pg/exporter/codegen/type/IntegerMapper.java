package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32;

public class IntegerMapper extends PgTypeMapper {
    public IntegerMapper() {
        super(List.of("pg_catalog.int2", "pg_catalog.int4"), TYPE_SINT32, "getInt");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String dbField, String protoVariable, String protoGetter) {
        return CodeBlock.of("$L.bind($S, (Integer) $L.$L());",
                queryVariable, dbField, protoVariable, protoGetter);
    }
}