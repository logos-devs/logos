package app.summer.module;

import app.summer.service.FeedService;
import dev.logos.app.App;
import dev.logos.app.AppModule;


public class SummerModule extends AppModule {
    public SummerModule() {
        this.bind(App.class).toInstance(
            App.builder()
               .name("Summer")
               .domain("summer.app")
               .build());

        this.addService(FeedService.class);
    }
}