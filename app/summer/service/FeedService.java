package app.summer.service;

import app.summer.proto.feed.FeedServiceGrpc;
import app.summer.proto.feed.GetFeedRequest;
import app.summer.proto.feed.GetFeedResponse;
import io.grpc.stub.StreamObserver;

public class FeedService extends FeedServiceGrpc.FeedServiceImplBase {
    @Override
    public void getFeed(GetFeedRequest request,
                        StreamObserver<GetFeedResponse> responseObserver) {
    }
}