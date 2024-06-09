package dev.logos.client.twilio;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.twilio.http.TwilioRestClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;


record TwilioCredentials(String account_sid, String auth_token) {
}

public class TwilioModule extends AbstractModule {

    private static final String SECRET_NAME = "dev/external/twilio";

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    public TwilioRestClient provideTwilioRestClient() {
        try (
            SecretsManagerClient secretsManager = SecretsManagerClient.builder().build();
        ) {
            GetSecretValueResponse response = secretsManager.getSecretValue(
                GetSecretValueRequest.builder().secretId(SECRET_NAME).build());

            TwilioCredentials twilioCredentials = new Gson().fromJson(response.secretString(), TwilioCredentials.class);
            return new TwilioRestClient.Builder(twilioCredentials.account_sid(),
                                                twilioCredentials.auth_token()).build();
        }
    }
}
