package dev.logos.service.backend.server;

import com.google.inject.Guice;
import com.google.inject.Inject;
import io.grpc.Server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;


public class ServerExecutor {

    private static final int TERMINATION_GRACE_PERIOD_SECONDS = 25;
    private final Logger logger;
    private final Set<Server> servers;

    @Inject
    public ServerExecutor(Set<Server> servers, Logger logger) {
        this.servers = servers;
        this.logger = logger;
    }

    public void start() {
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
                        throw new RuntimeException(e);
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

    public static void main(String[] args) throws IOException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, InterruptedException {
        ServerExecutor serverExecutor = Guice.createInjector(new ServerModule()).getInstance(ServerExecutor.class);
        serverExecutor.start();
        serverExecutor.blockUntilShutdown();
    }
}