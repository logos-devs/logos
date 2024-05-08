package dev.logos.user;

public class NotAuthenticated extends Exception {
    public NotAuthenticated(String message) {
        super(message);
    }
}