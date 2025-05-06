package dev.logos.service.backend.server.worker;

public enum WorkerState {
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    FAILED;

    public static WorkerState fromString(String state) {
        return WorkerState.valueOf(state.toUpperCase());
    }
}
