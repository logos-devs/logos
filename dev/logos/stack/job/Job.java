package dev.logos.stack.job;

import java.util.concurrent.CompletableFuture;

public interface Job {

    CompletableFuture<Job> start();

    CompletableFuture<Job> stop();

    String getName();

    JobState getState();
}