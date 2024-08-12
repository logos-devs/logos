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

    @Override
    public String getToken() throws NotAuthenticated {
        throw new NotAuthenticated("Anonymous user does not have a token");
    }
}