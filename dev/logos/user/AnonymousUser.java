package dev.logos.user;

public class AnonymousUser extends User {
    public AnonymousUser() {
    }

    @Override
    public String getDisplayName() {
        return "Anonymous";
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }
}