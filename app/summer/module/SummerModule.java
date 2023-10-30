package app.summer.module;

import app.summer.service.EntryStorageService;
import app.summer.service.SourceRssStorageService;
import app.summer.storage.summer.EntryStorageServiceGrpc;
import app.summer.storage.summer.EntryStorageServiceGrpc.EntryStorageServiceFutureStub;
import app.summer.storage.summer.SourceRssStorageServiceGrpc;
import app.summer.storage.summer.SourceRssStorageServiceGrpc.SourceRssStorageServiceFutureStub;
import com.google.inject.Provides;
import dev.logos.app.App;
import dev.logos.app.AppModule;
import io.grpc.ManagedChannel;


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
        addService(EntryStorageService.class);
        addService(SourceRssStorageService.class);
        super.configure();
    }

//    private <T extends AbstractFutureStub<T>> void addClient(Class<T> clazz) {
//        try {
//            bind(clazz).toInstance(clazz);
//        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
//    }

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