package app.auth.module;

import app.auth.interceptor.cognito.CognitoServerInterceptor;
import app.auth.module.data.CognitoHostConfig;
import app.auth.module.data.CognitoHostSecret;
import app.auth.service.CognitoService;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import dev.logos.app.AppModule;
import dev.logos.app.register.registerModule;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@registerModule
public class AuthModule extends AppModule {

    @Override
    protected void configure() {
//        addService(AuthService.class);
        addService(CognitoService.class);
        addInterceptor(CognitoServerInterceptor.class);
        super.configure();
    }

    @Provides
    SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .credentialsProvider(WebIdentityTokenFileCredentialsProvider.create())
                .build();
    }

    @Provides
    @Named("CognitoHostsJson")
    InputStream cognitoHostsJson() {
        return Objects.requireNonNull(
                getClass().getResourceAsStream(
                        "/dev/logos/stack/aws/cdk/cognito_hosts.json"));
    }

    @Provides
    @Named("CognitoUserPoolId")
    String getCognitoUserPoolId() {
        // TODO - get from cdk output
        return "us-east-2_0tayqImgc";
    }

    @Provides
    @Named("CognitoRegion")
    String getCognitoRegion() {
        return "us-east-2";
    }

    @Provides
    @Named("CognitoHosts")
    @SuppressWarnings("UnstableApiUsage")
    List<String> getCognitoHosts(
            @Named("CognitoHostsJson") InputStream cognitoHostsJson
    ) {
        return new Gson().fromJson(
                new InputStreamReader(cognitoHostsJson),
                new TypeToken<List<String>>() {
                }.getType()
        );
    }

    @Provides
    @Named("CognitoHostConfigsJson")
    InputStream cognitoServerConfigsJson() {
        return Objects.requireNonNull(
                getClass().getResourceAsStream(
                        "/dev/logos/stack/aws/cdk/cognito_server_config.json"));
    }

    @Provides
    @SuppressWarnings("UnstableApiUsage")
    Map<String, CognitoHostConfig> cognitoHostConfigs(
            @Named("CognitoHostConfigsJson") InputStream cognitoServerConfigsJson
    ) {
        return new Gson().fromJson(
                new InputStreamReader(cognitoServerConfigsJson),
                new TypeToken<Map<String, CognitoHostConfig>>() {
                }.getType()
        );
    }

    @Provides
    @SuppressWarnings("UnstableApiUsage")
    Map<String, CognitoHostSecret> getCognitoHostSecrets(
            @Named("CognitoHosts") List<String> cognitoHosts,
            Map<String, CognitoHostConfig> cognitoHostConfigs,
            SecretsManagerClient secretsManagerClient
    ) throws SecretsManagerException {
        Map<String, CognitoHostSecret> hostSecrets = new HashMap<>();

        for (String cognitoHost : cognitoHosts) {
            GetSecretValueRequest secretRequest =
                    GetSecretValueRequest.builder()
                            .secretId(cognitoHostConfigs.get(cognitoHost).clientCredentialsSecretArn())
                            .build();

            hostSecrets.put(cognitoHost,
                    new Gson().fromJson(
                            secretsManagerClient.getSecretValue(secretRequest).secretString(),
                            CognitoHostSecret.class));
        }
        return hostSecrets;
    }
}