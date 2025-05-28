package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64;

public class BigIntMapper extends PgTypeMapper {
    public BigIntMapper() {
        super(List.of("pg_catalog.int8"), TYPE_SINT64, "getLong");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String dbField, String protoVariable, String protoGetter) {
        return CodeBlock.of("$L.bind($S, (BigInteger) $L.$L());",
                queryVariable, dbField, protoVariable, protoGetter);
    }
}