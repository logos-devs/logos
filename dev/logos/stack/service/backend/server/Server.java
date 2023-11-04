package dev.logos.stack.service.backend.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import dev.logos.stack.job.Job;
import dev.logos.stack.job.JobState;
import dev.logos.stack.module.DatabaseModule;
import io.grpc.*;
import io.grpc.inprocess.InProcessServerBuilder;
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


class GlobalServerInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        return next.startCall(call, headers);
    }
}

public class Server implements Job {

    private JobState jobState = JobState.STOPPED;
    private static final int DEFAULT_PORT = 8081;
    private static final int TERMINATION_GRACE_PERIOD_SECONDS = 25;
    private final Logger logger;
    private final io.grpc.Server innerServer;
    private final io.grpc.Server outerServer;

    @Inject
    public Server(Set<BindableService> services, Logger logger) {
        this.logger = logger;

        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(DEFAULT_PORT);
        serverBuilder.intercept(new GlobalServerInterceptor());
        for (BindableService service : services) {
            serverBuilder.addService(service);
        }

        outerServer = serverBuilder.build();
        innerServer = InProcessServerBuilder.forName("logos-in-process").build();
    }

    public CompletableFuture<Job> start() {
        this.jobState = JobState.STARTING;

        return CompletableFuture.supplyAsync(() -> {
            try {
                outerServer.start();
                innerServer.start();
                this.jobState = JobState.RUNNING;
                logger.info("Server started, listening on " + outerServer.getPort());
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
                outerServer.shutdown().awaitTermination(
                    TERMINATION_GRACE_PERIOD_SECONDS,
                    TimeUnit.SECONDS);
                innerServer.shutdown().awaitTermination(
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
        outerServer.awaitTermination();
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
                System.err.println("MODULE: " + clazz.getCanonicalName());
                modules.add(clazz.getDeclaredConstructor().newInstance());
            }
        }

        Injector injector = Guice.createInjector(modules);
        Server server = injector.getInstance(Server.class);
        server.start();
        server.blockUntilShutdown();
    }
}