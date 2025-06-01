package dev.logos.service.backend.server;

import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import dev.logos.app.register.registerModule;
import dev.logos.service.Service;
import dev.logos.service.backend.server.worker.Worker;
import io.grpc.*;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


@registerModule
public class ServerModule extends AbstractModule {
    private static final int DEFAULT_PORT = 8081;

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AsyncClientExecutor {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ThreadPoolSize {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ServerThreadPool {
    }

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), Service.class);
        Multibinder.newSetBinder(binder(), ServerInterceptor.class);
        Multibinder.newSetBinder(binder(), Worker.class);
        OptionalBinder.newOptionalBinder(binder(), Key.get(Integer.class, ThreadPoolSize.class))
                      .setDefault().toInstance(32);
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
    @ServerThreadPool
    ExecutorService serverThreadPool(@ThreadPoolSize Integer threadPoolSize) {
        return Executors.newFixedThreadPool(threadPoolSize);
    }

    @Provides
    ServerExecutor provideServerExecutor(
            Set<Server> servers,
            Set<Worker> workers,
            Logger logger,
            @ServerThreadPool ExecutorService executorService) {
        return new ServerExecutor(servers, workers, logger, executorService);
    }

    @Provides
    public Set<Server> provideServers(
            Set<Service> services,
            Set<ServerInterceptor> interceptors,
            GuardServerInterceptor guardServerInterceptor,
            @ServerThreadPool ExecutorService serverThreadPool
    ) {
        ServerBuilder<?> innerServerBuilder = InProcessServerBuilder.forName("logos-in-process")
                                                                    .executor(serverThreadPool);
        ServerBuilder<?> outerServerBuilder = ServerBuilder.forPort(DEFAULT_PORT)
                                                           .executor(serverThreadPool);

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
