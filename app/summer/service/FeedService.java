package app.summer.service;

import app.summer.proto.feed.Feed;
import app.summer.proto.feed.FeedServiceGrpc.FeedServiceImplBase;
import app.summer.proto.feed.GetFeedRequest;
import app.summer.proto.feed.GetFeedResponse;
import app.summer.proto.feed.Source;
import app.summer.storage.summer.EntryStorageServiceGrpc.EntryStorageServiceFutureStub;
import app.summer.storage.summer.ListEntryRequest;
import app.summer.storage.summer.ListEntryResponse;
import app.summer.storage.summer.ListSourceRssRequest;
import app.summer.storage.summer.ListSourceRssResponse;
import app.summer.storage.summer.SourceRssStorageServiceGrpc.SourceRssStorageServiceFutureStub;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ExecutionException;

public class FeedService extends FeedServiceImplBase {
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
    public void getFeed(
            GetFeedRequest request,
            StreamObserver<GetFeedResponse> responseObserver
    ) {
        ListenableFuture<ListSourceRssResponse> listSourceRssFetch =
                sourceRssStorageService.list(ListSourceRssRequest.newBuilder().build());

        ListenableFuture<ListEntryResponse> listEntryFetch =
                entryStorageService.list(ListEntryRequest.newBuilder().build());

        try {
            Feed feed = Feed.newBuilder()
                            .addAllEntry(listEntryFetch.get().getResultsList())
                            .addAllSource(
                                    listSourceRssFetch.get().getResultsList().stream().map(
                                                              sourceRss -> Source.newBuilder()
                                                                                 .setId(sourceRss.getId())
                                                                                 .setIcon(sourceRss.getFaviconUrl())
                                                                                 .build())
                                                      .toList())
                            .build();

            responseObserver.onNext(GetFeedResponse.newBuilder().setFeed(feed).build());
            responseObserver.onCompleted();

        } catch (InterruptedException | ExecutionException err) {
            responseObserver.onError(err);
        }
    }
}
