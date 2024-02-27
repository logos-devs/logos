package dev.logos.service.storage;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import dev.logos.service.Service;
import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.exceptions.EntityWriteException;
import dev.logos.service.storage.pg.Select;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.stream.Stream;

public interface EntityStorageService<
    ListRequest extends GeneratedMessageV3,
    ListResponse extends GeneratedMessageV3,
    CreateRequest extends GeneratedMessageV3,
    CreateResponse extends GeneratedMessageV3,
    UpdateRequest extends GeneratedMessageV3,
    UpdateResponse extends GeneratedMessageV3,
    DeleteRequest extends GeneratedMessageV3,
    DeleteResponse extends GeneratedMessageV3,
    Entity extends GeneratedMessageV3,
    StorageIdentifier
> extends Service {

    EntityStorage<Entity, StorageIdentifier> getStorage();
    Select.Builder query(ListRequest listRequest);

    default <Request extends GeneratedMessageV3> Entity entity(Request ignoredRequest) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    default <Request> StorageIdentifier id(Request ignoredRequest) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    // TODO move this shameful code to its proper place in the pg<->protobuf converter
    default UUID bytestringToUuid(ByteString byteString) {
        if (byteString.size() != 16) {
            throw new IllegalArgumentException("Invalid UUID byte length: " + byteString.size());
        }

        long mostSigBits = byteString.substring(0, 8).asReadOnlyByteBuffer().getLong();
        long leastSigBits = byteString.substring(8, 16).asReadOnlyByteBuffer().getLong();

        return new UUID(mostSigBits, leastSigBits);
    }

    // TODO move this shameful code to its proper place in the pg<->protobuf converter
    default ByteString uuidToByteString(UUID uuid) {
        byte[] bytes = new byte[16];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return ByteString.copyFrom(bytes);
    }

    interface RequestHandler<Response> {
        Response handle() throws EntityReadException, EntityWriteException;
    }

    default ListResponse response(Stream<Entity> ignoredEntityStream, ListRequest ignoredRequest) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    default <Req, Resp> Resp response(StorageIdentifier ignoredId, Req ignoredRequest) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    default <Request, Response> void request(Request request, StreamObserver<Response> responseObserver, RequestHandler<Response> handler) {
        if (guard(request, responseObserver)) { return; }

        try {
            responseObserver.onNext(handler.handle());
            responseObserver.onCompleted();
        } catch (EntityReadException | EntityWriteException e) {
            responseObserver.onError(
                    Status.UNAVAILABLE.withCause(e).asRuntimeException());
        }
    }

    default void list(ListRequest listRequest, StreamObserver<ListResponse> responseObserver) {
        request(listRequest, responseObserver, () -> {
            try (Stream<Entity> entryListStream = getStorage().query(query(listRequest))) {
                return response(entryListStream, listRequest);
            }
        });
    }

    default void create(CreateRequest createRequest, StreamObserver<CreateResponse> responseObserver) {
        request(createRequest, responseObserver, () ->
            response(getStorage().create(entity(createRequest)), createRequest));
    }

    default void update(UpdateRequest updateRequest, StreamObserver<UpdateResponse> responseObserver) {
        request(updateRequest, responseObserver, () ->
                response(getStorage().update(id(updateRequest), entity(updateRequest)), updateRequest));
    }

    default void delete(DeleteRequest deleteRequest, StreamObserver<DeleteResponse> responseObserver) {
        request(deleteRequest, responseObserver, () ->
                response(getStorage().delete(id(deleteRequest)), deleteRequest));
    }
}
