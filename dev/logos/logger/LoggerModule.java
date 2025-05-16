package dev.logos.logger;

import com.google.inject.AbstractModule;
import dev.logos.app.register.registerModule;

@registerModule
public class LoggerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(LoggerFactory.class).to(LoggerFactoryImpl.class);
    }
}