package dev.logos.service.storage;

import com.google.protobuf.ByteString;
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

public interface EntityStorageService<ListRequest, ListResponse, CreateRequest, CreateResponse,
                                      UpdateRequest, UpdateResponse, DeleteRequest, DeleteResponse,
                                      Entity, StorageIdentifier> {

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

    default <T> void error(StreamObserver<T> responseObserver, String description) {
        responseObserver.onError(
                Status.INVALID_ARGUMENT
                        .withDescription(description)
                        .asException());
    }

    default UUID bytestringToUuid(ByteString byteString) {
        if (byteString.size() != 16) {
            throw new IllegalArgumentException("Invalid UUID byte length: " + byteString.size());
        }

        long mostSigBits = byteString.substring(0, 8).asReadOnlyByteBuffer().getLong();
        long leastSigBits = byteString.substring(8, 16).asReadOnlyByteBuffer().getLong();

        return new UUID(mostSigBits, leastSigBits);
    }

    default ByteString uuidToByteString(UUID uuid) {
        byte[] bytes = new byte[16];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return ByteString.copyFrom(bytes);
    }

    default boolean allow(User user, Object request) {
        return false;
    }

    default boolean validate(Object request, Validator validator) { return true; }

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

    default void create(CreateRequest createRequest,
                        StreamObserver<CreateResponse> responseObserver) {

        if (guard(createRequest, responseObserver)) { return; }

        try {
            Entity entity = entity(createRequest);
            StorageIdentifier id = getStorage().create(entity);
            responseObserver.onNext(createResponse(id, createRequest));
            responseObserver.onCompleted();
        } catch (EntityWriteException e) {
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Creation failed.")
                            .asRuntimeException());
        }
    }

    default void list(ListRequest listRequest,
                      StreamObserver<ListResponse> responseObserver) {

        if (guard(listRequest, responseObserver)) { return; }

        try (Stream<Entity> entryListStream = getStorage().query(query(listRequest))) {
            responseObserver.onNext(listResponse(entryListStream, listRequest));
            responseObserver.onCompleted();
        } catch (EntityReadException e) {
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Lookup failed.")
                            .asRuntimeException());
        }
    }

    default void update(UpdateRequest updateRequest,
                        StreamObserver<UpdateResponse> responseObserver) {

        if (guard(updateRequest, responseObserver)) { return; }

        try {
            Entity entity = entity(updateRequest);
            StorageIdentifier id = getStorage().update(id(updateRequest), entity);
            responseObserver.onNext(updateResponse(id, updateRequest));
            responseObserver.onCompleted();
        } catch (EntityWriteException e) {
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Update failed.")
                            .asRuntimeException());
        }
    }

    default void delete(DeleteRequest deleteRequest,
                        StreamObserver<DeleteResponse> responseObserver) {

        if (guard(deleteRequest, responseObserver)) { return; }

        try {
            StorageIdentifier id = getStorage().delete(id(deleteRequest));
            responseObserver.onNext(deleteResponse(id, deleteRequest));
            responseObserver.onCompleted();
        } catch (EntityWriteException e) {
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Delete failed.")
                            .asRuntimeException());
        }
    }
}
