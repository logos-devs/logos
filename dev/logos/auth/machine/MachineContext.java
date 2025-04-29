package dev.logos.auth.machine;

import io.grpc.Context;

import java.util.Optional;

public class MachineContext {
    public static final Context.Key<Machine> MACHINE_CONTEXT_KEY = Context.key("machine");

    public static Optional<Machine> getCallingMachine() {
        return Optional.ofNullable(MACHINE_CONTEXT_KEY.get());
    }
}
