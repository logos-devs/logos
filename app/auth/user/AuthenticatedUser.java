package app.auth.user;

import dev.logos.user.User;
import io.jsonwebtoken.Claims;

public class AuthenticatedUser extends User {
    private final String id;

    public AuthenticatedUser(Claims claims) {
        this.id = claims.get("username", String.class);
    }

    @Override
    public String getDisplayName() {
        return this.id;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }
}
