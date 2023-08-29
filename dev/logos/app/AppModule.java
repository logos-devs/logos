package dev.logos.app;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.grpc.BindableService;

public abstract class AppModule extends AbstractModule {
    public void addService(Class<? extends BindableService> serviceClass) {
        Multibinder
                .newSetBinder(binder(), BindableService.class)
                .addBinding().to(serviceClass);
    }
}