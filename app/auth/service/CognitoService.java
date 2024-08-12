package app.auth.service;

import app.auth.module.data.CognitoHostConfig;
import app.auth.module.data.CognitoHostSecret;
import app.auth.proto.cognito.CognitoServiceGrpc;
import app.auth.proto.cognito.ProcessAuthCodeRequest;
import app.auth.proto.cognito.ProcessAuthCodeResponse;
import com.google.gson.Gson;
import com.google.inject.Inject;
import dev.logos.service.Service;
import dev.logos.user.User;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import software.amazon.awssdk.http.HttpStatusCode;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.SEVERE;

public class CognitoService extends CognitoServiceGrpc.CognitoServiceImplBase implements Service {
    private final Logger logger;

    private static final String TOKEN_REQUEST_FAILURE_MSG = "Failed to retrieve a token.";

    private final Map<String, CognitoHostConfig> cognitoHostConfigs;
    private final Map<String, CognitoHostSecret> cognitoHostSecrets;

    @Inject
    public CognitoService(
        Logger logger,
        Map<String, CognitoHostConfig> cognitoHostConfigs,
        Map<String, CognitoHostSecret> cognitoHostSecrets
    ) {
        this.logger = logger;
        this.cognitoHostConfigs = cognitoHostConfigs;
        this.cognitoHostSecrets = cognitoHostSecrets;
    }

    private void onFailedRequest(
        StreamObserver<?> responseObserver,
        Level level,
        String msg,
        Object obj
    ) {
        logger.log(level, msg, obj);
        responseObserver.onError(
            Status.INVALID_ARGUMENT
                .withDescription(msg)
                .asRuntimeException());
    }

    record Tokens(
        String access_token,
        String id_token,
        String refresh_token,
        String token_type,
        int expires_in
    ) {
        Tokens {
            requireNonNull(access_token);
            requireNonNull(id_token);
            requireNonNull(refresh_token);
            requireNonNull(token_type);
        }
    }

    @Override
    public <Req> boolean allow(Req request, User ignoredUser) {
        return request instanceof ProcessAuthCodeRequest;
    }

    @Override
    public void processAuthCode(ProcessAuthCodeRequest request, StreamObserver<ProcessAuthCodeResponse> responseObserver) {
        String domain = "dev.summer.app";

        CognitoHostConfig cognitoHostConfig = cognitoHostConfigs.get(domain);
        CognitoHostSecret cognitoHostSecret = cognitoHostSecrets.get(domain);

        HttpPost tokenRequest = new HttpPost();
        tokenRequest.setURI(URI.create(cognitoHostConfig.baseUrl() + "/oauth2/token"));
        tokenRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");

        List<BasicNameValuePair> tokenRequestPayload = List.of(
            new BasicNameValuePair("grant_type", "authorization_code"),
            new BasicNameValuePair("code", request.getAuthCode()),
            new BasicNameValuePair("client_id", cognitoHostSecret.clientId()),
            new BasicNameValuePair("client_secret", cognitoHostSecret.clientSecret()),
            new BasicNameValuePair("redirect_uri", cognitoHostConfig.redirectUrl()));

        try {
            tokenRequest.setEntity(new UrlEncodedFormEntity(tokenRequestPayload));
        } catch (UnsupportedEncodingException e) {
            onFailedRequest(responseObserver, SEVERE, TOKEN_REQUEST_FAILURE_MSG, e);
            return;
        }

        String json;
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse tokenResponse = client.execute(tokenRequest)
        ) {
            if (tokenResponse.getStatusLine().getStatusCode() != HttpStatusCode.OK) {
                onFailedRequest(responseObserver, SEVERE, TOKEN_REQUEST_FAILURE_MSG,
                                EntityUtils.toString(tokenResponse.getEntity()));
                return;
            }

            json = EntityUtils.toString(tokenResponse.getEntity());
        } catch (IOException e) {
            onFailedRequest(responseObserver, SEVERE, TOKEN_REQUEST_FAILURE_MSG, e);
            return;
        }

        Tokens tokens = new Gson().fromJson(json, Tokens.class);

        responseObserver.onNext(ProcessAuthCodeResponse.newBuilder()
                                                       .setAccessToken(tokens.access_token)
                                                       .setRefreshToken(tokens.refresh_token)
                                                       .setIdToken(tokens.id_token)
                                                       .setExpiresIn(tokens.expires_in)
                                                       .build());
        responseObserver.onCompleted();
    }
}