package dev.logos.auth.principal;

public interface Principal {
    String getDisplayName();

    boolean isAuthenticated();

    String getId();

    String getToken();
}
