package dev.logos.auth.user;


public abstract class User {
    public static User get() {
        return UserContext.getCurrentUser().orElseThrow();
    }

    public abstract String getDisplayName();

    public abstract boolean isAuthenticated();

    public abstract String getId();

    public abstract String getEmail();

    public abstract String getToken();
}