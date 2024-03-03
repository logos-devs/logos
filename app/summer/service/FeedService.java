package app.summer.service;

import app.summer.proto.feed.Feed;
import app.summer.proto.feed.FeedServiceGrpc.FeedServiceImplBase;
import app.summer.proto.feed.GetFeedRequest;
import app.summer.proto.feed.GetFeedResponse;
import app.summer.proto.feed.Source;
import app.summer.storage.summer.EntryStorageServiceGrpc.EntryStorageServiceFutureStub;
import app.summer.storage.summer.ListEntryRequest;
import app.summer.storage.summer.ListSourceRssRequest;
import app.summer.storage.summer.SourceRssStorageServiceGrpc.SourceRssStorageServiceFutureStub;
import com.google.inject.Inject;
import dev.logos.service.Service;
import dev.logos.user.User;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ExecutionException;

public class FeedService extends FeedServiceImplBase implements Service {
    private final SourceRssStorageServiceFutureStub sourceRssStorageService;
    private final EntryStorageServiceFutureStub entryStorageService;

    @Inject
    public FeedService(
            SourceRssStorageServiceFutureStub sourceRssStorageService,
            EntryStorageServiceFutureStub entryStorageService
    ) {
        this.sourceRssStorageService = sourceRssStorageService;
        this.entryStorageService = entryStorageService;
    }

    @Override
    public <Req> boolean allow(Req request, User user) {
        return request instanceof GetFeedRequest;
    }

    @Override
    public void getFeed(
            GetFeedRequest request,
            StreamObserver<GetFeedResponse> responseObserver
    ) {
        try {
            responseObserver.onNext(
                    GetFeedResponse.newBuilder().setFeed(
                            Feed.newBuilder()
                                .addAllEntry(
                                        entryStorageService.list(ListEntryRequest.newBuilder().build())
                                                           .get()
                                                           .getResultsList())
                                .addAllSource(
                                        sourceRssStorageService.list(ListSourceRssRequest.newBuilder().build())
                                                               .get()
                                                               .getResultsList().stream().map(
                                                                       sourceRss -> Source.newBuilder()
                                                                                          .setId(sourceRss.getId())
                                                                                          .setIcon(sourceRss.getFaviconUrl())
                                                                                          .build())
                                                               .toList())
                                .build()).build());
            responseObserver.onCompleted();

        } catch (InterruptedException | ExecutionException err) {
            responseObserver.onError(err);
        }
    }
}
