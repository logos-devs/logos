package dev.logos.service.storage.pg;

import com.google.protobuf.ByteString;

import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.UUID;

public class Converter {
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

    public static OffsetDateTime stringToOffsetDateTime(String string) {
        return OffsetDateTime.parse(string);
    }

    public static String offsetDateTimeToString(OffsetDateTime offsetDateTime) {
        return offsetDateTime.toString();
    }
}
