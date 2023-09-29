package app.summer.module;

import app.summer.service.EntryStorageService;
import app.summer.service.SourceRssStorageService;
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
        super.configure();
    }
}