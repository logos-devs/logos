package dev.logos.service.storage;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.exceptions.EntityWriteException;
import dev.logos.service.storage.pg.Select;
import dev.logos.service.storage.validator.Validator;
import dev.logos.user.User;
import dev.logos.user.UserContext;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.stream.Stream;

interface SecureService {
    default boolean allow(User user, Object request) {
        return false;
    }

    default boolean validate(Object request, Validator validator) {
        return true;
    }

    default <T> void error(StreamObserver<T> responseObserver, String description) {
        responseObserver.onError(
                Status.INVALID_ARGUMENT
                        .withDescription(description)
                        .asException());
    }

    default <Req, Resp> boolean guard(Req request, StreamObserver<Resp> responseObserver) {
        if (!allow(UserContext.getCurrentUser(), request)) {
            error(responseObserver, "Access denied.");
            return true;
        }

        Validator validator = new Validator();
        if (!validate(request, validator)) {
            error(responseObserver, "Invalid request.");
            return true;
        }

        return false;
    }
}

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
> extends SecureService {

    EntityStorage<Entity, StorageIdentifier> getStorage();
    Select.Builder query(ListRequest listRequest);

    default Entity entity(Object request) {
        throw new UnsupportedOperationException("Not implemented.");
    };

    default StorageIdentifier id(Object request) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    ListResponse listResponse(Stream<Entity> entityStream, ListRequest listRequest);
    CreateResponse createResponse(StorageIdentifier id, CreateRequest createRequest);
    UpdateResponse updateResponse(StorageIdentifier id, UpdateRequest updateRequest);
    DeleteResponse deleteResponse(StorageIdentifier id, DeleteRequest deleteRequest);

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

    default <Request, Response> void handleRequest(Request request, StreamObserver<Response> responseObserver, RequestHandler<Response> handler) {
        if (guard(request, responseObserver)) { return; }

        try {
            responseObserver.onNext(handler.handle());
            responseObserver.onCompleted();
        } catch (EntityReadException | EntityWriteException e) {
            responseObserver.onError(
                    Status.UNAVAILABLE.withCause(e).asRuntimeException());
        }
    }

    default void list(ListRequest listRequest,
                      StreamObserver<ListResponse> responseObserver) {
        handleRequest(listRequest, responseObserver, () -> {
            try (Stream<Entity> entryListStream = getStorage().query(query(listRequest))) {
                return listResponse(entryListStream, listRequest);
            }
        });
    }

    default void create(CreateRequest createRequest,
                        StreamObserver<CreateResponse> responseObserver) {
        handleRequest(createRequest, responseObserver, () ->
            createResponse(getStorage().create(entity(createRequest)), createRequest));
    }

    default void update(UpdateRequest updateRequest,
                        StreamObserver<UpdateResponse> responseObserver) {
        handleRequest(updateRequest, responseObserver, () ->
                updateResponse(getStorage().update(id(updateRequest), entity(updateRequest)), updateRequest));
    }

    default void delete(DeleteRequest deleteRequest,
                        StreamObserver<DeleteResponse> responseObserver) {
        handleRequest(deleteRequest, responseObserver, () ->
                deleteResponse(getStorage().delete(id(deleteRequest)), deleteRequest));
    }
}
