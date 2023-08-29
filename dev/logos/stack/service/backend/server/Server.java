package dev.logos.stack.service.backend.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import dev.logos.stack.job.Job;
import dev.logos.stack.job.JobState;
import dev.logos.stack.module.DatabaseModule;
import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


public class Server implements Job {

    private JobState jobState = JobState.STOPPED;
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static final int DEFAULT_PORT = 8081;
    private static final int TERMINATION_GRACE_PERIOD_SECONDS = 25;
    private final io.grpc.Server server;

    @Inject
    public Server(Set<BindableService> services) {
        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(DEFAULT_PORT);
        for (BindableService service : services) {
            serverBuilder.addService(service);
        }

        server = serverBuilder.build();
    }

    public CompletableFuture<Job> start() {
        this.jobState = JobState.STARTING;

        return CompletableFuture.supplyAsync(() -> {
            try {
                server.start();
                this.jobState = JobState.RUNNING;
                logger.info("Server started, listening on " + server.getPort());
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.err.println("*** shutting down gRPC server since JVM is shutting down");
                    Server.this.stop().thenApply(job -> {
                        System.err.println("*** server shut down");
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
        this.jobState = JobState.STOPPING;

        return CompletableFuture.supplyAsync(() -> {
            try {
                server.shutdown().awaitTermination(
                    TERMINATION_GRACE_PERIOD_SECONDS,
                    TimeUnit.SECONDS);
                this.jobState = JobState.STOPPED;
            } catch (InterruptedException e) {
                this.jobState = JobState.RUNTIME_FAILURE;
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
        server.awaitTermination();
    }

    public static void main(String[] args)
        throws IOException, InterruptedException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {

        List<AbstractModule> modules = new ArrayList<>();
        modules.add(new DatabaseModule());
        modules.add(new ServerModule());

        String parentPackageName = "app";
        Reflections reflections = new Reflections(parentPackageName, Scanners.SubTypes);

        for (Class<? extends AbstractModule> clazz : reflections.getSubTypesOf(AbstractModule.class)) {
            String packageName = clazz.getPackageName();
            if (packageName.startsWith(parentPackageName) && !Modifier.isAbstract(clazz.getModifiers())) {
                logger.info("APP: " + clazz.getCanonicalName());
                modules.add(clazz.getDeclaredConstructor().newInstance());
            }
        }

        Server server = Guice.createInjector(modules).getInstance(Server.class);
        server.start();
        server.blockUntilShutdown();
    }
}