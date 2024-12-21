package app.auth.cognito.module.data;

import static java.util.Objects.requireNonNull;

public record CognitoClientCredentialsSecret(
        String clientId,
        String clientSecret
) {
    public CognitoClientCredentialsSecret {
        requireNonNull(clientId);
        requireNonNull(clientSecret);
    }
}
