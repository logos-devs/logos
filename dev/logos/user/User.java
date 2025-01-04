package dev.logos.user;


public abstract class User {
    public abstract String getDisplayName();

    public abstract boolean isAuthenticated();

    public abstract String getId() throws NotAuthenticated;

    public abstract String getEmail() throws NotAuthenticated;

    public abstract String getToken() throws NotAuthenticated;
}