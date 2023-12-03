package app.auth.module.data;

import static java.util.Objects.requireNonNull;

public record CognitoHostSecret(
        String clientId,
        String clientSecret
) {
    public CognitoHostSecret {
        requireNonNull(clientId);
        requireNonNull(clientSecret);
    }
}
