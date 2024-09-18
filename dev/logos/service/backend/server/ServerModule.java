package dev.logos.service.backend.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import dev.logos.app.register.registerModule;
import dev.logos.service.Service;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@registerModule
public class ServerModule extends AbstractModule {
    private static final int DEFAULT_PORT = 8081;

    @Override
    protected void configure() {
        Multibinder
                .newSetBinder(binder(), ServerInterceptor.class)
                .addBinding().to(GuardServerInterceptor.class);

        Multibinder
                .newSetBinder(binder(), ClientInterceptor.class)
                .addBinding().to(AuthTokenForwardingInterceptor.class);

        Multibinder.newSetBinder(binder(), Service.class);
    }

    @Provides
    public ManagedChannel provideManagedChannel(Set<ClientInterceptor> clientInterceptors) {
        /* TODO select in-process vs network channel with offload logic */
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName("logos-in-process");

        for (ClientInterceptor interceptor : clientInterceptors) {
            channelBuilder.intercept(interceptor);
        }

        return channelBuilder.build();
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
    public Set<Server> provideServers(Set<Service> services, Set<ServerInterceptor> interceptors) {
        ServerBuilder<?> innerServerBuilder = InProcessServerBuilder.forName("logos-in-process");
        ServerBuilder<?> outerServerBuilder = ServerBuilder.forPort(DEFAULT_PORT);

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
