package app.auth.cognito.user;

import dev.logos.user.NotAuthenticated;
import dev.logos.user.User;
import io.jsonwebtoken.Claims;

import static java.lang.System.err;
import static java.util.Objects.requireNonNull;

// TODO: rename to CognitoUser
public class AuthenticatedUser extends User {
    private final String id;
    private final String name;
    private final String email;
    private final String token;

    public AuthenticatedUser(String token, Claims claims) {
        this.id = requireNonNull(claims.get("sub", String.class));
        this.email = requireNonNull(claims.get("email", String.class));
        this.name = requireNonNull(claims.get("name", String.class));
        this.token = token;
    }

    @Override
    public String getId() throws NotAuthenticated {
        return this.id;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getEmail() throws NotAuthenticated {
        return email;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public String getToken() throws NotAuthenticated {
        return token;
    }
}
