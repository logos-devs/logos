package app.summer.module;

import app.summer.service.EntryStorageService;
import app.summer.storage.EntryStorage;
import dev.logos.app.App;
import dev.logos.app.AppModule;


public class SummerModule extends AppModule {
    @Override
    protected void configure() {

        bind(App.class).toInstance(
                App.builder()
                        .name("Summer")
                        .domain("summer.app")
                        .build());


        bind(EntryStorage.class).toInstance(new EntryStorage());

        addService(EntryStorageService.class);
        super.configure();
    }
}