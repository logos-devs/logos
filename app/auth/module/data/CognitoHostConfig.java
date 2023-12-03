package app.auth.module.data;

import static java.util.Objects.requireNonNull;

public record CognitoHostConfig(
        String baseUrl,
        String redirectUrl,
        String clientCredentialsSecretArn
) {
    public CognitoHostConfig {
        requireNonNull(baseUrl);
        requireNonNull(redirectUrl);
        requireNonNull(clientCredentialsSecretArn);
    }
}
