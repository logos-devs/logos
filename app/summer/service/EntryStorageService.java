package app.summer.service;

import app.summer.storage.EntryStorage;
import app.summer.storage.summer.ListEntryRequest;
import app.summer.storage.summer.ListEntryResponse;
import app.summer.storage.summer.EntryStorageServiceGrpc;
import com.google.inject.Inject;
import dev.logos.stack.service.storage.exceptions.EntityReadException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class EntryStorageService extends EntryStorageServiceGrpc.EntryStorageServiceImplBase {

    private final EntryStorage entryStorage;

    @Inject
    public EntryStorageService(EntryStorage entryStorage) {
        this.entryStorage = entryStorage;
    }

    @Override
    public void listEntry(
            ListEntryRequest request,
            StreamObserver<ListEntryResponse> responseObserver
    ) {
        try {
            responseObserver.onNext(
                    ListEntryResponse
                            .newBuilder()
                            .addAllResults(entryStorage.list().toList())
                            .build());
        } catch (EntityReadException e) {
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Lookup failed.")
                            .asRuntimeException());
        }
        responseObserver.onCompleted();
    }
}
