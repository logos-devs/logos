package dev.logos.stack.job;

public enum JobState {
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    STARTUP_FAILURE,
    RUNTIME_FAILURE,
}
