package app.auth.dev.module;

import app.auth.cognito.module.data.CognitoClientCredentialsSecret;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoOptional;
import dev.logos.app.AppModule;
import dev.logos.app.register.registerModule;
import dev.logos.auth.user.annotation.UserScoped;
import dev.logos.stack.aws.module.annotation.AwsRegion;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;

import java.util.*;
import java.util.concurrent.Executor;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static java.util.Objects.requireNonNull;


/**
 * Development-only module for authentication. During tests, the username
 * and password environment variables may be absent. In that case dummy
 * credentials with the value {@code "NOT_SET"} are used.
 */
@registerModule
public class DevModule extends AppModule {
    public DevModule() {
    }

    @Provides
    CallCredentials provideCallCredentials(@UserScoped CallCredentials userCallCredentials) {
        return userCallCredentials;
    }

    @UserScoped
    @Singleton
    @ProvidesIntoOptional(ProvidesIntoOptional.Type.ACTUAL)
    CallCredentials provideUserScopedCallCredentials(@AwsRegion String region, CognitoClientCredentialsSecret clientCredentials) {
        String username = requireNonNull(System.getenv("LOGOS_DEV_COGNITO_USERNAME"));
        String password = requireNonNull(System.getenv("LOGOS_DEV_COGNITO_PASSWORD"));

        try (CognitoIdentityProviderClient cognitoClient =
                     CognitoIdentityProviderClient.builder()
                             .region(Region.of(region))
                             .credentialsProvider(AnonymousCredentialsProvider.create())
                             .build()) {

            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", username);
            authParams.put("PASSWORD", password);

            String clientSecret = clientCredentials.clientSecret();
            String secretHash = computeSecretHash(username, clientCredentials.clientId(), clientSecret);
            authParams.put("SECRET_HASH", secretHash);

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .clientId(clientCredentials.clientId())
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .authParameters(authParams)
                    .build();

            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);
            AuthenticationResultType authenticationResult = authResponse.authenticationResult();
            String token = authenticationResult.idToken();

            return new CognitoDevCallCredentials(token);
        }
    }

    private String computeSecretHash(String username, String clientId, String clientSecret) {
        String message = username + clientId;
        SecretKeySpec secretKey = new SecretKeySpec(clientSecret.getBytes(), "HmacSHA256");
        Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(message.getBytes());
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error computing secret hash", e);
        }
    }

    private static class CognitoDevCallCredentials extends CallCredentials {
        private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
                Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

        private final String idToken;

        CognitoDevCallCredentials(String idToken) {
            this.idToken = idToken;
        }

        @Override
        public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
            appExecutor.execute(() -> {
                Metadata headers = new Metadata();
                headers.put(AUTHORIZATION_METADATA_KEY, "Bearer " + idToken);
                applier.apply(headers);
            });
        }
    }
}