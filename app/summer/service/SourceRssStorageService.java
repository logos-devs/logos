package app.summer.service;

import app.summer.storage.SourceRssStorage;
import app.summer.storage.summer.ListSourceRssRequest;
import app.summer.storage.summer.ListSourceRssResponse;
import app.summer.storage.summer.SourceRss;
import app.summer.storage.summer.SourceRssStorageServiceGrpc;
import com.google.inject.Inject;
import dev.logos.stack.service.storage.exceptions.EntityReadException;
import dev.logos.stack.service.storage.pg.Select;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.stream.Stream;

import static app.summer.storage.Summer.SourceRss.sourceRss;

public class SourceRssStorageService extends SourceRssStorageServiceGrpc.SourceRssStorageServiceImplBase {

    private final SourceRssStorage sourceRssStorage;

    @Inject
    public SourceRssStorageService(SourceRssStorage sourceRssStorage) {
        this.sourceRssStorage = sourceRssStorage;
    }

    @Override
    public void listSourceRss(
            ListSourceRssRequest request,
            StreamObserver<ListSourceRssResponse> responseObserver
    ) {
        try (Stream<SourceRss> sourceRssListStream = sourceRssStorage.query(Select.builder().from(sourceRss))) {
            responseObserver.onNext(
                    ListSourceRssResponse
                            .newBuilder()
                            .addAllResults(sourceRssListStream.toList())
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