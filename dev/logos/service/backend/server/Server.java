package dev.logos.service.backend.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import dev.logos.service.Service;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessServerBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import static java.lang.System.err;
import static java.util.concurrent.TimeUnit.SECONDS;


public class Server {

    private static final int DEFAULT_PORT = 8081;
    private static final int TERMINATION_GRACE_PERIOD_SECONDS = 25;
    private final Logger logger;
    private final io.grpc.Server innerServer;
    private final io.grpc.Server outerServer;

    @Inject
    public Server(Set<Service> services, Set<ServerInterceptor> interceptors, Logger logger) {
        this.logger = logger;

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

        innerServer = innerServerBuilder.build();
        outerServer = outerServerBuilder.build();
    }

    public void start() {
        try {
            outerServer.start();
            innerServer.start();
            logger.info("Server started, listening on " + outerServer.getPort());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    outerServer.shutdown();
                    innerServer.shutdown();
                    this.blockUntilShutdown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                err.println("*** server shut down");
            }));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        outerServer.awaitTermination(TERMINATION_GRACE_PERIOD_SECONDS, SECONDS);
        innerServer.awaitTermination(TERMINATION_GRACE_PERIOD_SECONDS, SECONDS);
    }

    public static void main(String[] args)
        throws IOException, InterruptedException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {

        List<AbstractModule> modules = new ArrayList<>();

        Enumeration<URL> en = Server.class.getClassLoader().getResources("META-INF");
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            JarURLConnection urlcon = (JarURLConnection) (url.openConnection());
            try (JarFile jar = urlcon.getJarFile()) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement().getName();
                    if (entry.contains("META-INF/app-modules-")) {
                        try (InputStream inputStream = Server.class.getClassLoader().getResourceAsStream(entry)) {
                            assert inputStream != null;
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    Class<?> clazz = Class.forName(line);
                                    err.println("MODULE: " + clazz.getCanonicalName());
                                    modules.add((AbstractModule) clazz.getDeclaredConstructor().newInstance());
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        Injector injector = Guice.createInjector(modules);
        Server server = injector.getInstance(Server.class);
        server.start();
        server.blockUntilShutdown();
    }
}