package dev.logos.service.storage;

import com.google.protobuf.GeneratedMessage;
import dev.logos.service.Service;
import dev.logos.service.storage.exceptions.StorageException;
import dev.logos.auth.user.NotAuthenticated;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.stream.Stream;

public interface StorageService extends Service {

//    default <Request> Select.Builder query(Request request, Select.Builder select) {
//        return select;
//    }
//
//    <Request> Select.Builder query(Request request);
//
//    <Request> Entity entity(Request ignoredRequest) throws NotAuthenticated;
//
//    <Request> Object id(Request request);
//
//    default <Request, Response> void request(Request request, StreamObserver<Response> responseObserver, RequestHandler<Response> handler) {
//        try {
//            responseObserver.onNext(handler.handle());
//            responseObserver.onCompleted();
//        } catch (EntityReadException | EntityWriteException | NotAuthenticated e) {
//            responseObserver.onError(
//                    Status.INVALID_ARGUMENT.withCause(e).withDescription(e.getMessage()).asRuntimeException()
//            );
//
//            onFailedRequest(responseObserver, e.getMessage(), e);
//        }
//    }
//
//    <Request, Response> Response response(Stream<Entity> entityStream, Request request);
//
//    <Request, Response> Response response(StorageIdentifier id, Request request);
//
//    interface RequestHandler<Response> {
//        Response handle() throws EntityReadException, EntityWriteException, NotAuthenticated;
//    }
//
//    default void list(ListRequest listRequest, StreamObserver<ListResponse> responseObserver) {
//        request(listRequest, responseObserver, () -> {
//            try (Stream<Entity> entryListStream = getStorage().query(query(listRequest))) {
//                return response(entryListStream, listRequest);
//            }
//        });
//    }
}