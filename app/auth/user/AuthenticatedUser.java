package app.auth.user;

import dev.logos.user.NotAuthenticated;
import dev.logos.user.User;
import io.jsonwebtoken.Claims;

public class AuthenticatedUser extends User {
    private final String id;
    private final String token;

    public AuthenticatedUser(String token, Claims claims) {
        this.id = claims.get("username", String.class);
        this.token = token;
    }

    @Override
    public String getDisplayName() {
        return this.id;
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
