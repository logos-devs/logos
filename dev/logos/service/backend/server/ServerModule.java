package dev.logos.service.backend.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import dev.logos.service.Service;
import dev.logos.service.backend.interceptor.GuardServerInterceptor;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.lang.System.err;

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

        try {
            Enumeration<URL> en = ServerModule.class.getClassLoader().getResources("META-INF");
            while (en.hasMoreElements()) {
                URL url = en.nextElement();
                JarURLConnection urlcon = (JarURLConnection) (url.openConnection());
                try (JarFile jar = urlcon.getJarFile()) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String entry = entries.nextElement().getName();
                        if (entry.contains("META-INF/app-modules-")) {
                            try (InputStream inputStream = ServerModule.class.getClassLoader()
                                                                             .getResourceAsStream(entry)) {
                                assert inputStream != null;
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        Class<?> clazz = Class.forName(line);
                                        err.println("MODULE: " + clazz.getCanonicalName());
                                        install((AbstractModule) clazz.getDeclaredConstructor().newInstance());
                                    }
                                }
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        } catch (IOException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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
