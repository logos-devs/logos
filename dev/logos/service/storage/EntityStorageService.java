package dev.logos.service.storage;

import com.google.protobuf.GeneratedMessage;
import dev.logos.service.Service;
import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.exceptions.EntityWriteException;
import dev.logos.service.storage.pg.Select;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.stream.Stream;

public interface EntityStorageService<
        ListRequest extends GeneratedMessage,
        ListResponse extends GeneratedMessage,
        CreateRequest extends GeneratedMessage,
        CreateResponse extends GeneratedMessage,
        UpdateRequest extends GeneratedMessage,
        UpdateResponse extends GeneratedMessage,
        DeleteRequest extends GeneratedMessage,
        DeleteResponse extends GeneratedMessage,
        Entity extends GeneratedMessage,
        StorageIdentifier
        > extends Service {

    EntityStorage<Entity, StorageIdentifier> getStorage();

    Select.Builder query(ListRequest listRequest) throws EntityReadException;

    <Request> Entity entity(Request ignoredRequest);

    <Request> StorageIdentifier id(Request request);

    default ListResponse response(Stream<Entity> ignoredEntityStream, ListRequest ignoredRequest) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    default <Req extends GeneratedMessage, Resp extends GeneratedMessage> Resp response(StorageIdentifier ignoredId, Req ignoredRequest) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    default <Request, Response> void request(Request request, StreamObserver<Response> responseObserver, RequestHandler<Response> handler) {
        try {
            responseObserver.onNext(handler.handle());
            responseObserver.onCompleted();
        } catch (EntityReadException | EntityWriteException e) {
            responseObserver.onError(
                    Status.UNAVAILABLE.withCause(e).asRuntimeException());
        }
    }

    interface RequestHandler<Response> {
        Response handle() throws EntityReadException, EntityWriteException;
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
