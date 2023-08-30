package app.summer.service;

import app.summer.storage.EntryStorage;
import app.summer.storage.summer.Entry;
import app.summer.storage.summer.EntryStorageServiceGrpc;
import app.summer.storage.summer.ListEntryRequest;
import app.summer.storage.summer.ListEntryResponse;
import com.google.inject.Inject;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.stream.Stream;

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
        try (Stream<Entry> entryListStream = entryStorage.list()) {
            responseObserver.onNext(
                    ListEntryResponse
                            .newBuilder()
                            .addAllResults(entryListStream.toList())
                            .build());
        } catch (Exception e) {
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Lookup failed.")
                            .asRuntimeException());
        }
        responseObserver.onCompleted();
    }
}
