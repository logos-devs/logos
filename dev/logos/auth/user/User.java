package dev.logos.auth.user;


import dev.logos.auth.principal.Principal;

import java.util.Optional;

public abstract class User implements Principal {
    public static User get() {
        return UserContext.getCurrentUser().orElseThrow();
    }

    public Optional<String> getEmail() {
        return Optional.empty();
    }
}