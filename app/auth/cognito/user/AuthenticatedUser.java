package app.auth.cognito.user;

import dev.logos.user.NotAuthenticated;
import dev.logos.user.User;
import io.jsonwebtoken.Claims;

import java.util.Objects;

import static java.lang.System.err;

public class AuthenticatedUser extends User {
    private final String id;
    private final String token;
    private final String displayName;

    public AuthenticatedUser(String token, Claims claims) {
        err.println(claims);
        this.id = Objects.requireNonNull(claims.get("cognito:username", String.class));
        this.displayName = Objects.requireNonNull(claims.get("email", String.class));
        this.token = token;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public String getToken() throws NotAuthenticated {
        return this.token;
    }
}
