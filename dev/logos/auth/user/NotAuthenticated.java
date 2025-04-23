package dev.logos.auth.user;

public class NotAuthenticated extends Exception {
    public NotAuthenticated(String message) {
        super(message);
    }
}