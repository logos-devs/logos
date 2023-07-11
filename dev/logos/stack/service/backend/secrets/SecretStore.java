package dev.logos.stack.service.backend.secrets;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

enum Secrets {
    TwilioSid("twilio_sid"),
    TwilioAuthToken("twilio_auth_token");

    public final String name;

    private Secrets(String name) {
        this.name = name;
    }
}

// TODO : Test secret access at instantiation to prevent permissions errors at runtime.
//  Ideally this catches access issues at build-time.
public class SecretStore {

    private final Environment environment;
    private final App app;

    public SecretStore(Environment environment,
                       App app) {
        this.environment = environment;
        this.app = app;
    }

    public static SecretsManagerClient getClient() {
        return SecretsManagerClient.builder()
                                   .region(Region.US_EAST_2)
                                   .credentialsProvider(DefaultCredentialsProvider.create())
                                   .build();
    }

    public String getValue(String secretName) throws dev.logos.stack.service.backend.secrets.SecretRetrievalFailure {
        try (SecretsManagerClient client = getClient()) {
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                                                                      .secretId(String.format(
                                                                          "%s/%s/%s",
                                                                          this.environment.name,
                                                                          this.app.name,
                                                                          secretName))
                                                                      .build();

            return client.getSecretValue(valueRequest).secretString();

        } catch (SecretsManagerException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw new dev.logos.stack.service.backend.secrets.SecretRetrievalFailure();
        }
    }

    public String getTwilioSid() throws dev.logos.stack.service.backend.secrets.SecretRetrievalFailure {
        return this.getValue(Secrets.TwilioSid.name);
    }

    public String getTwilioAuthToken() throws dev.logos.stack.service.backend.secrets.SecretRetrievalFailure {
        return this.getValue(Secrets.TwilioAuthToken.name);
    }
}
