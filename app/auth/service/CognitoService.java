package app.auth.service;

import app.auth.proto.cognito.CognitoServiceGrpc;
import app.auth.proto.cognito.ProcessAuthCodeRequest;
import app.auth.proto.cognito.ProcessAuthCodeResponse;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CognitoService extends CognitoServiceGrpc.CognitoServiceImplBase {
    private static final String TOKEN_REQUEST_FAILURE_MSG = "Failed to retrieve a token.";
    private final CognitoIdentityProviderClient cognitoClient;
    private final SecretsManagerClient secretsClient;
    private final Map<String, Map<String, String>> cognitoSecretHostMap;
    private static final Type cognitoSecretHostMapType = new TypeToken<Map<String, Map<String, String>>>() {
    }.getType();
    private static final Type cognitoSecretPayloadType = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final Type cognitoTokenResponseType = new TypeToken<Map<String, String>>() {
    }.getType();
    private final Logger logger;

    @Inject
    public CognitoService(Logger logger) {
        this.logger = logger;

        cognitoClient = CognitoIdentityProviderClient.builder().build();
        secretsClient = SecretsManagerClient.builder().build();

        try {
            cognitoSecretHostMap = new Gson().fromJson(
                    new FileReader("dev/logos/infra/cognito_secret_host_map.json"),
                    cognitoSecretHostMapType
            );
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    public Map<String, String> getCognitoHostSecret(String domain) throws SecretsManagerException {
        return new Gson().fromJson(this.secretsClient.getSecretValue(
                GetSecretValueRequest.builder().secretId(
                        cognitoSecretHostMap.get(domain).get("clientCredentialsSecretArn")
                ).build()
        ).secretString(), cognitoSecretPayloadType);
        // System.err.println(e.awsErrorDetails().errorMessage());
        // System.exit(1);
    }

    @Override
    public void processAuthCode(ProcessAuthCodeRequest request, StreamObserver<ProcessAuthCodeResponse> responseObserver) {
        String domain = "dev.summer.app";
        Map<String, String> cognitoHostSecret = getCognitoHostSecret(domain);
        Map<String, String> cognitoSecretHostMap = this.cognitoSecretHostMap.get(domain);

        String clientId = Objects.requireNonNull(cognitoHostSecret.get("clientId"));
        String clientSecret = Objects.requireNonNull(cognitoHostSecret.get("clientSecret"));
        String loginBaseUrl = Objects.requireNonNull(cognitoSecretHostMap.get("baseUrl"));
        String loginRedirectUrl = Objects.requireNonNull(cognitoSecretHostMap.get("redirectUrl"));

        HttpPost tokenRequest = new HttpPost();
        tokenRequest.setURI(URI.create(loginBaseUrl + "/oauth2/token"));
        tokenRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");

        List<BasicNameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("client_secret", clientSecret));
        params.add(new BasicNameValuePair("code", request.getAuthCode()));
        params.add(new BasicNameValuePair("redirect_uri", loginRedirectUrl));

        try {
            tokenRequest.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, "Failed to encode tokenRequest entity payload.", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(TOKEN_REQUEST_FAILURE_MSG).asRuntimeException()
            );
            return;
        }

        try (
                CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse tokenResponse = client.execute(tokenRequest);
        ) {
            if (tokenResponse.getStatusLine().getStatusCode() != 200) {
                System.out.println(EntityUtils.toString(tokenResponse.getEntity()));
                logger.log(Level.SEVERE, "Failed to retrieve a token.");
                responseObserver.onError(
                        Status.INVALID_ARGUMENT.withDescription(TOKEN_REQUEST_FAILURE_MSG).asRuntimeException()
                );
                return;
            }

            Map<String, String> tokenResponseMap = new Gson().fromJson(
                    EntityUtils.toString(tokenResponse.getEntity()),
                    cognitoTokenResponseType
            );

            // String tokenType = Objects.requireNonNull(tokenResponseMap.get("token_type"));

            ProcessAuthCodeResponse response =
                    ProcessAuthCodeResponse
                            .newBuilder()
                            .setAccessToken(Objects.requireNonNull(tokenResponseMap.get("access_token")))
                            .setIdToken(Objects.requireNonNull(tokenResponseMap.get("id_token")))
                            .setRefreshToken(Objects.requireNonNull(tokenResponseMap.get("refresh_token")))
                            .setExpiresIn(Integer.parseInt(Objects.requireNonNull(tokenResponseMap.get("expires_in"))))
                            .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
