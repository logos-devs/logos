package dev.logos.app;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.grpc.BindableService;

public abstract class AppModule extends AbstractModule {
    private final Multibinder<BindableService> serviceBinder;

    public AppModule() {
        serviceBinder = Multibinder.newSetBinder(binder(), BindableService.class);
    }

    public void addService(Class<? extends BindableService> serviceClass) {
        this.serviceBinder.addBinding().to(serviceClass);
    }
}