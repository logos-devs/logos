package app.summer.module;

import app.summer.service.EntryStorageService;
import app.summer.service.FeedService;
import app.summer.service.SourceImapStorageService;
import app.summer.service.SourceRssStorageService;
import app.summer.storage.summer.EntryStorageServiceGrpc;
import app.summer.storage.summer.EntryStorageServiceGrpc.EntryStorageServiceFutureStub;
import app.summer.storage.summer.SourceRssStorageServiceGrpc;
import app.summer.storage.summer.SourceRssStorageServiceGrpc.SourceRssStorageServiceFutureStub;
import com.google.inject.Provides;
import dev.logos.app.App;
import dev.logos.app.AppModule;
import dev.logos.module.registerModule;
import io.grpc.ManagedChannel;


@registerModule
public class SummerModule extends AppModule {
    @Provides
    public App provideApp() {
        return App.builder()
                  .name("Summer")
                  .domain("summer.app")
                  .build();
    }

    @Override
    protected void configure() {
        addService(FeedService.class);
        addService(EntryStorageService.class);
        addService(SourceImapStorageService.class);
        addService(SourceRssStorageService.class);
    }

    @Provides
    public EntryStorageServiceFutureStub provideEntryStorageServiceStub(ManagedChannel managedChannel) {
        return EntryStorageServiceFutureStub.newStub(
            (channel, callOptions) -> EntryStorageServiceGrpc.newFutureStub(managedChannel),
            managedChannel);
    }

    @Provides
    public SourceRssStorageServiceFutureStub provideSourceRssStorageServiceStub(ManagedChannel managedChannel) {
        return SourceRssStorageServiceFutureStub.newStub(
            (channel, callOptions) -> SourceRssStorageServiceGrpc.newFutureStub(managedChannel),
            managedChannel);
    }
}