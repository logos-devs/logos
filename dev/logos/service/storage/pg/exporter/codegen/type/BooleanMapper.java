package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL;

public class BooleanMapper extends PgTypeMapper {
    public BooleanMapper() {
        super(List.of("pg_catalog.bool", "pg_catalog.boolean"), TYPE_BOOL, "getBoolean");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String dbField, String protoVariable, String protoGetter) {
        return CodeBlock.of("$L.bind($S, (Boolean) $L.$L());",
                queryVariable, dbField, protoVariable, protoGetter);
    }
}