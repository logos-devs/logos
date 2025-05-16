package dev.logos.service.storage;

import com.google.protobuf.GeneratedMessage;
import dev.logos.service.Service;
import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.exceptions.EntityWriteException;
import dev.logos.service.storage.pg.Select;
import dev.logos.auth.user.NotAuthenticated;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.stream.Stream;


public interface EntityStorageService<
        GetRequest extends GeneratedMessage,
        GetResponse extends GeneratedMessage,
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

    default <Request> Select.Builder query(Request request, Select.Builder select) {
        return select;
    }

    <Request> Select.Builder query(Request request);

    <Request> Entity entity(Request ignoredRequest) throws NotAuthenticated;

    <Request> Object id(Request request);

    default <Request, Response> void request(Request request, StreamObserver<Response> responseObserver, RequestHandler<Response> handler) {
        try {
            responseObserver.onNext(handler.handle());
            responseObserver.onCompleted();
        } catch (EntityReadException | EntityWriteException | NotAuthenticated e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withCause(e).withDescription(e.getMessage()).asRuntimeException()
            );

            onFailedRequest(responseObserver, e.getMessage(), e);
        }
    }

    <Request, Response> Response response(Stream<Entity> entityStream, Request request);

    <Request, Response> Response response(StorageIdentifier id, Request request);

    interface RequestHandler<Response> {
        Response handle() throws EntityReadException, EntityWriteException, NotAuthenticated;
    }

    default void get(GetRequest getRequest, StreamObserver<GetResponse> responseObserver) {
        request(getRequest, responseObserver, () -> {
            try (Stream<Entity> entryGetStream = getStorage().query(id(getRequest), query(getRequest))) {
                return response(entryGetStream, getRequest);
            }
        });
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
