package dev.logos.app;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import dev.logos.service.Service;
import io.grpc.ServerInterceptor;

public abstract class AppModule extends AbstractModule {
    public void addService(Class<? extends Service> serviceClass) {
        Multibinder
                .newSetBinder(binder(), Service.class)
                .addBinding().to(serviceClass);
    }

    public void addInterceptor(Class<? extends ServerInterceptor> interceptorClass) {
        Multibinder
                .newSetBinder(binder(), ServerInterceptor.class)
                .addBinding().to(interceptorClass);
    }
}