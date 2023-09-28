package app.summer.module;

import app.summer.service.EntryStorageService;
import app.summer.service.SourceRssStorageService;
import app.summer.storage.EntryStorage;
import app.summer.storage.SourceRssStorage;
import com.google.inject.Provides;
import dev.logos.app.App;
import dev.logos.app.AppModule;


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

        addStorage(EntryStorage.class);
        addStorage(SourceRssStorage.class);

        super.configure();
    }
}