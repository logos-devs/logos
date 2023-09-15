package app.summer.service;

import app.summer.storage.EntryStorage;
import app.summer.storage.summer.Entry;
import app.summer.storage.summer.EntryStorageServiceGrpc;
import app.summer.storage.summer.ListEntryRequest;
import app.summer.storage.summer.ListEntryResponse;
import com.google.inject.Inject;
import dev.logos.stack.service.storage.exceptions.EntityReadException;
import dev.logos.stack.service.storage.pg.Filter;
import dev.logos.stack.service.storage.pg.OrderBy;
import dev.logos.stack.service.storage.pg.Select;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.stream.Stream;

import static app.summer.storage.Summer.Entry.ParentId.parentId;
import static app.summer.storage.Summer.Entry.PublishedAt.publishedAt;
import static app.summer.storage.Summer.Entry.entry;
import static dev.logos.stack.service.storage.pg.Filter.Op.IS_NULL;
import static dev.logos.stack.service.storage.pg.SortOrder.DESC;

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
        try (Stream<Entry> entryListStream = entryStorage.query(
                Select.builder()
                        .from(entry)
                        .limit(50)
                        .where(List.of(
                                Filter.builder()
                                        .column(parentId)
                                        .op(IS_NULL)
                                        .build()))
                        .orderBy(OrderBy.builder()
                                .column(publishedAt)
                                .direction(DESC))
        )) {
            responseObserver.onNext(
                    ListEntryResponse
                            .newBuilder()
                            .addAllResults(entryListStream.toList())
                            .build());
            responseObserver.onCompleted();
        } catch (EntityReadException e) {
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Lookup failed.")
                            .asRuntimeException());
        }
    }
}