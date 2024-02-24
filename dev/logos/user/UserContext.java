package dev.logos.user;

import io.grpc.Context;

public class UserContext {
    public static final Context.Key<User> USER_CONTEXT_KEY = Context.key("user");

    public static User getCurrentUser() {
        return USER_CONTEXT_KEY.get();
    }
}
