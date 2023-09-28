package dev.logos.stack.service.storage;

import dev.logos.stack.service.storage.exceptions.EntityReadException;
import dev.logos.stack.service.storage.pg.Select;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.stream.Stream;

public interface EntityStorageService<Request, Response, Entity> {
    Select.Builder listQuery(Request request);
    EntityStorage<Entity> getStorage();
    Response result(Stream<Entity> entityListStream);

    default void listHandler(Request request,
                      StreamObserver<Response> responseObserver) {
        try (Stream<Entity> entryListStream = getStorage().query(listQuery(request))) {
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
