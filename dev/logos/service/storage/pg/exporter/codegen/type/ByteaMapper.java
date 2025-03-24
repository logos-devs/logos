package dev.logos.service.storage.pg.exporter.codegen.type;

import com.google.protobuf.ByteString;
import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES;

public class ByteaMapper extends PgTypeMapper {
    public ByteaMapper() {
        super(List.of("bytea"), TYPE_BYTES, "getBytes");
    }

    @Override
    public CodeBlock pgToProto(CodeBlock innerCall) {
        return CodeBlock.of("$T.copyFrom($L)", ByteString.class, innerCall);
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
        return CodeBlock.of("$L.bind($S, (($T) $L.get($S)).toByteArray());",
                queryVariable, fieldName, ByteString.class, fieldVariable, fieldName);
    }
}