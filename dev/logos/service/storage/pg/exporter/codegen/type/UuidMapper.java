package dev.logos.service.storage.pg.exporter.codegen.type;

import com.google.protobuf.ByteString;
import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.Converter;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES;

public class UuidMapper extends PgTypeMapper {
    public UuidMapper() {
        super(List.of("uuid"), TYPE_BYTES, "getObject");
    }

    @Override
    public String resultSetFieldCast() {
        return "(java.util.UUID)";
    }

    @Override
    public CodeBlock pgToProto(CodeBlock innerCall) {
        return CodeBlock.of("$T.uuidToBytestring($L)", Converter.class, innerCall);
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName) {
        return CodeBlock.of("$L.bind($S, $T.bytestringToUuid(($T) $L.get($S)));",
                queryVariable, fieldName, Converter.class, ByteString.class, fieldVariable, fieldName);
    }
}