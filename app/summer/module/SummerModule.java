package app.summer.module;

import app.summer.service.EntryStorageService;
import app.summer.service.SourceRssStorageService;
import app.summer.storage.EntryStorage;
import app.summer.storage.SourceRssStorage;
import app.summer.storage.summer.Entry;
import app.summer.storage.summer.SourceRss;
import com.google.inject.TypeLiteral;
import dev.logos.app.App;
import dev.logos.app.AppModule;
import dev.logos.stack.service.storage.EntityStorage;


public class SummerModule extends AppModule {
    @Override
    protected void configure() {

        bind(App.class).toInstance(
                App.builder()
                        .name("Summer")
                        .domain("summer.app")
                        .build());

        addService(EntryStorageService.class);
        addService(SourceRssStorageService.class);

        bind(new TypeLiteral<EntityStorage<Entry>>(){}).toInstance(new EntryStorage());
        bind(new TypeLiteral<EntityStorage<SourceRss>>(){}).toInstance(new SourceRssStorage());

        super.configure();
    }
}