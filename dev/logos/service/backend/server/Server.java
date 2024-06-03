package dev.logos.service.backend.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import dev.logos.job.Job;
import dev.logos.job.JobState;
import dev.logos.service.Service;
import dev.logos.service.storage.module.DatabaseModule;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessServerBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static dev.logos.job.JobState.*;
import static java.lang.System.err;
import static java.util.concurrent.TimeUnit.SECONDS;


public class Server implements Job {

    private JobState jobState = STOPPED;
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

    public CompletableFuture<Job> start() {
        this.jobState = STARTING;

        return CompletableFuture.supplyAsync(() -> {
            try {
                outerServer.start();
                innerServer.start();
                this.jobState = RUNNING;
                logger.info("Server started, listening on " + outerServer.getPort());
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    err.println("*** shutting down gRPC server since JVM is shutting down");
                    Server.this.stop().thenApply(job -> {
                        err.println("*** server shut down");
                        return job;
                    });
                }));
                return Server.this;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Job> stop() {
        this.jobState = STOPPING;

        return CompletableFuture.supplyAsync(() -> {
            try {
                outerServer.shutdown().awaitTermination(
                    TERMINATION_GRACE_PERIOD_SECONDS,
                    SECONDS);
                innerServer.shutdown().awaitTermination(
                    TERMINATION_GRACE_PERIOD_SECONDS,
                    SECONDS);
                this.jobState = STOPPED;
            } catch (InterruptedException e) {
                this.jobState = RUNTIME_FAILURE;
                throw new RuntimeException(e);
            }
            return Server.this;
        });
    }

    @Override
    public String getName() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public JobState getState() {
        return this.jobState;
    }

    private void blockUntilShutdown() throws InterruptedException {
        outerServer.awaitTermination();
        innerServer.awaitTermination();
    }

    public static void main(String[] args)
        throws IOException, InterruptedException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {

        List<AbstractModule> modules = new ArrayList<>();
        modules.add(new DatabaseModule());
        modules.add(new ServerModule());

        try (InputStream inputStream = Server.class.getClassLoader().getResourceAsStream("META-INF/app-modules.txt")) {
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

//        String parentPackageName = "app";
//        Reflections reflections = new Reflections(parentPackageName, Scanners.SubTypes);
//
//        for (Class<? extends AbstractModule> clazz : reflections.getSubTypesOf(AbstractModule.class)) {
//            String packageName = clazz.getPackageName();
//            if (packageName.startsWith(parentPackageName) && !Modifier.isAbstract(clazz.getModifiers())) {
//                err.println("MODULE: " + clazz.getCanonicalName());
//                modules.add(clazz.getDeclaredConstructor().newInstance());
//            }
//        }

        Injector injector = Guice.createInjector(modules);
        Server server = injector.getInstance(Server.class);
        server.start();
        server.blockUntilShutdown();
    }
}