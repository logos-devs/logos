package dev.logos.stack.service.storage;

import dev.logos.stack.service.storage.exceptions.EntityReadException;
import dev.logos.stack.service.storage.pg.Select;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.stream.Stream;

public interface EntityStorageService<Request, Response, Entity> {
    Select.Builder query(Request request);
    EntityStorage<Entity> getStorage();
    Response result(Stream<Entity> entityListStream);

    default boolean validate(Request request,
                             StreamObserver<Response> responseObserver) {
        return true;
    }

    default void validationError(StreamObserver<Response> responseObserver) {
        responseObserver.onError(
                Status.INVALID_ARGUMENT
                        .withDescription(".")
                        .asException());
    }

    default void listHandler(Request request,
                      StreamObserver<Response> responseObserver) {
        if (!validate(request, responseObserver)) {
            validationError(responseObserver);
            return;
        }

        try (Stream<Entity> entryListStream = getStorage().query(query(request))) {
            responseObserver.onNext(result(entryListStream));
            responseObserver.onCompleted();
        } catch (EntityReadException e) {
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Lookup failed.")
                            .asRuntimeException());
        }
    }
}
