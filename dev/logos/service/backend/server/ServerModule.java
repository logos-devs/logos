package dev.logos.service.backend.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import dev.logos.service.Service;
import dev.logos.service.backend.interceptor.GuardServerInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessChannelBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServerModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder
                .newSetBinder(binder(), ServerInterceptor.class)
                .addBinding().to(GuardServerInterceptor.class);
    }

    @Provides
    public ManagedChannel provideManagedChannel() {
        /* TODO select channel with offload logic */
        return InProcessChannelBuilder.forName("logos-in-process").build();
    }

    @Provides
    public Map<String, Service> provideServiceMap(Set<Service> services) {
        Map<String, Service> serviceMap = new HashMap<>();

        for (Service service : services) {
            serviceMap.put(service.bindService().getServiceDescriptor().getName(), service);
        }

        return serviceMap;
    }
}
