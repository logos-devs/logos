package dev.logos.service.backend.server;

import com.google.inject.Inject;
import com.google.inject.Injector;
import dev.logos.module.ModuleLoader;
import dev.logos.service.backend.server.worker.Worker;
import io.grpc.Server;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;


public class ServerExecutor {

    private static final int TERMINATION_GRACE_PERIOD_SECONDS = 25;
    private final Logger logger;
    private final Set<Server> servers;
    private final Set<Worker> workers;

    @Inject
    public ServerExecutor(Set<Server> servers, Set<Worker> workers, Logger logger) {
        this.servers = servers;
        this.workers = workers;
        this.logger = logger;
    }

    public void start() {
        for (Worker worker : workers) {
            worker.start().addListener(
                    () -> logger.info("Worker started: %s %s".formatted(worker.getId(), worker.getName())),
                    Runnable::run);
        }

        try {
            for (Server server : servers) {
                server.start();
                logger.info("Server %s started".formatted(server));

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    logger.info("Server %s shutting down due to JVM shutdown".formatted(server));
                    try {
                        server.shutdown();
                        server.awaitTermination(TERMINATION_GRACE_PERIOD_SECONDS, SECONDS);
                    } catch (InterruptedException e) {
                        logger.log(Level.SEVERE, "Server %s interrupted during shutdown: %s".formatted(server, e.getMessage()), e);
                    }
                    for (Worker worker : workers) {
                        try {
                            worker.stop().get();
                        } catch (InterruptedException | ExecutionException e) {
                            logger.log(Level.SEVERE, "Failed to stop worker %s: %s".formatted(worker.getName(), e.getMessage()), e);
                        }
                    }
                    logger.info("Server %s has shut down".formatted(server));
                }));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        for (Server server : servers) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) {
        try {
            Injector injector = ModuleLoader.createInjector();
            ServerExecutor serverExecutor = injector.getInstance(ServerExecutor.class);
            serverExecutor.start();
            serverExecutor.blockUntilShutdown();
        } catch (Exception e) {
            // stack traces can get lost inside Guice.
            e.printStackTrace();
            System.exit(1);
        }
    }
}