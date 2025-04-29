package dev.logos.auth.user;

import io.grpc.Context;

import java.util.Optional;

public class UserContext {
    public static final Context.Key<User> USER_CONTEXT_KEY = Context.key("user");

    public static Optional<User> getCurrentUser() {
        return Optional.ofNullable(USER_CONTEXT_KEY.get());
    }
}