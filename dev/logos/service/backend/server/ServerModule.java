package dev.logos.service.backend.server;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import dev.logos.service.Service;
import dev.logos.service.backend.interceptor.GuardServerInterceptor;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.lang.System.err;


public class ServerModule extends AbstractModule {
    private static final int DEFAULT_PORT = 8081;
    private static final String CONFIG_PATH = getRequiredEnv("LOGOS_SERVICE_JAR_CONFIG_PATH");
    private static final String JAR_DIR = getRequiredEnv("LOGOS_JAR_DIR");
    private static final String META_INF_DIR = "META-INF";
    private static final String APP_MODULE_PREFIX = META_INF_DIR + "/app-modules-";
    private static final Logger logger = LoggerFactory.getLogger(ServerModule.class);

    @Override
    protected void configure() {
        Multibinder
            .newSetBinder(binder(), ServerInterceptor.class)
            .addBinding().to(GuardServerInterceptor.class);

        Multibinder
            .newSetBinder(binder(), ClientInterceptor.class)
            .addBinding().to(AuthTokenForwardingInterceptor.class);

        try {
            URLClassLoader urlClassLoader = new URLClassLoader(
                    readConfigMap().stream().map(jarPathStr -> {
                        Path jarPath = Paths.get(JAR_DIR, jarPathStr);
                        if (!Files.exists(jarPath)) {
                            throw new IllegalStateException("Jar file not found: " + jarPath);
                        }

                        if (!Files.isReadable(jarPath)) {
                            throw new IllegalStateException("Jar file is not readable: " + jarPath);
                        }
                        return jarPath.toUri();
                    }).map(uri -> {
                        try {
                            return uri.toURL();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).toArray(URL[]::new)
            );

            discoverModules(urlClassLoader);
        } catch (IOException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void discoverModules(ClassLoader classLoader) throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Enumeration<URL> resources = classLoader.getResources(META_INF_DIR);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            logger.atInfo().addKeyValue("jarUrl", url).log("Loading service jar");

            JarURLConnection connection = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    if (entryName.startsWith(APP_MODULE_PREFIX)) {
                        logger.atInfo().addKeyValue("jarEntry", entryName).log("Loading jar entry");

                        try (InputStream inputStream = classLoader.getResourceAsStream(entryName)) {
                            if (inputStream != null) {
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        try {
                                            Class<?> clazz = Class.forName(line, true, classLoader);
                                            logger.atInfo().addKeyValue("module", clazz.getCanonicalName()).log("Loading module");
                                            install((AbstractModule) clazz.getDeclaredConstructor().newInstance());
                                        } catch (ClassNotFoundException e) {
                                            logger.atError().setCause(e).addKeyValue("class", line).log("Error loading class");
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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

    private static String getRequiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Required environment variable " + key + " is not set");
        }
        return value;
    }

    private List<String> readConfigMap() {
        try (Reader reader = Files.newBufferedReader(Paths.get(CONFIG_PATH))) {
            return Arrays.asList(new Gson().fromJson(reader, String[].class));
        } catch (IOException e) {
            logger.atError().setCause(e).addKeyValue("CONFIG_PATH", CONFIG_PATH).log("Apps configMap file not found");
            throw new RuntimeException(e);
        }
    }
}
