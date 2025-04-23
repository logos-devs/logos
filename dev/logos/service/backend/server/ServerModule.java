package dev.logos.service.backend.server;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import dev.logos.app.register.registerModule;
import dev.logos.service.Service;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


@registerModule
public class ServerModule extends AbstractModule {
    private static final int DEFAULT_PORT = 8081;

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AsyncClientExecutor {
    }

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), Service.class);
        Multibinder.newSetBinder(binder(), ServerInterceptor.class);
    }

    @Provides
    public GuardServerInterceptor provideGuardServerInterceptor(Map<String, Service> serviceMap) {
        return new GuardServerInterceptor(serviceMap);
    }

    @Provides
    @Singleton
    @AsyncClientExecutor
    public Executor provideAsyncClientExecutor() {
        return Executors.newWorkStealingPool();
    }

    @Provides
    public Map<String, Service> provideServiceMap(Set<Service> services) {
        Map<String, Service> serviceMap = new HashMap<>();

        for (Service service : services) {
            serviceMap.put(service.bindService().getServiceDescriptor().getName(), service);
        }

        return serviceMap;
    }

    @Provides
    public Set<Server> provideServers(
            Set<Service> services,
            Set<ServerInterceptor> interceptors,
            GuardServerInterceptor guardServerInterceptor
    ) {
        ServerBuilder<?> innerServerBuilder = InProcessServerBuilder.forName("logos-in-process");
        ServerBuilder<?> outerServerBuilder = ServerBuilder.forPort(DEFAULT_PORT);

        outerServerBuilder.addService(new HealthStatusManager().getHealthService());

        // GuardServerInterceptor must be first so it can use context set by other interceptors, because
        // counterintuitively interceptors are executed opposite to the order they are added. This is because each
        // interceptor wraps the next one in the chain like an onion.
        innerServerBuilder.intercept(guardServerInterceptor);
        outerServerBuilder.intercept(guardServerInterceptor);

        for (ServerInterceptor interceptor : interceptors) {
            innerServerBuilder.intercept(interceptor);
            outerServerBuilder.intercept(interceptor);
        }

        for (Service service : services) {
            innerServerBuilder.addService(service);
            outerServerBuilder.addService(service);
        }

        return Set.of(innerServerBuilder.build(), outerServerBuilder.build());
    }
}
