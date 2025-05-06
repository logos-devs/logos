package dev.logos.service.backend.server.worker;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.UUID;

public interface Worker {
    ListenableFuture<WorkerState> start();

    ListenableFuture<WorkerState> stop();

    ListenableFuture<WorkerState> restart();

    String getName();

    UUID getId();
}
