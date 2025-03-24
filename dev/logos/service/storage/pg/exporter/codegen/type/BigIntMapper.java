package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64;

public class BigIntMapper extends PgTypeMapper {
    public BigIntMapper() {
        super(List.of("bigint"), TYPE_SINT64, "getLong");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
        return CodeBlock.of("$L.bind($S, (BigInteger) $L.get($S));",
                queryVariable, fieldName, fieldVariable, fieldName);
    }
}