package dev.logos.service.storage.pg.exporter.codegen.type;

import com.google.protobuf.ByteString;
import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES;

public class UuidMapper extends PgTypeMapper {
    public UuidMapper() {
        super(List.of("pg_catalog.uuid"), TYPE_BYTES, "getObject");
    }

    @Override
    public String resultSetFieldCast() {
        return "(java.util.UUID)";
    }

    public static UUID bytestringToUuid(ByteString byteString) {
        if (byteString.size() != 16) {
            throw new IllegalArgumentException("Invalid UUID byte length: " + byteString.size());
        }

        long mostSigBits = byteString.substring(0, 8).asReadOnlyByteBuffer().getLong();
        long leastSigBits = byteString.substring(8, 16).asReadOnlyByteBuffer().getLong();

        return new UUID(mostSigBits, leastSigBits);
    }

    public static ByteString uuidToBytestring(UUID uuid) {
        byte[] bytes = new byte[16];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return ByteString.copyFrom(bytes);
    }

    @Override
    public CodeBlock pgToProto(CodeBlock innerCall) {
        return CodeBlock.of("$T.uuidToBytestring($L)", UuidMapper.class, innerCall);
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String dbField, String protoVariable, String protoGetter) {
        return CodeBlock.of("$L.bind($S, $T.bytestringToUuid(($T) $L.$L()));",
                queryVariable, dbField, UuidMapper.class, ByteString.class, protoVariable, protoGetter);
    }
}