package app.auth.cognito.service;

import app.auth.cognito.interceptor.cookie.CookieServerInterceptor;
import app.auth.cognito.module.annotation.AuthenticationCookieDomain;
import app.auth.cognito.module.data.CognitoClientCredentialsSecret;
import app.auth.cognito.module.data.CognitoStackOutputs;
import app.auth.proto.cognito.*;
import com.google.gson.Gson;
import com.google.inject.Inject;
import dev.logos.service.Service;
import dev.logos.user.User;
import dev.logos.user.UserContext;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import software.amazon.awssdk.http.HttpStatusCode;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class CognitoService extends CognitoServiceGrpc.CognitoServiceImplBase implements Service {
    private static final String TOKEN_REQUEST_FAILURE_MSG = "Failed to retrieve a token.";
    private final CognitoStackOutputs cognitoStackOutputs;
    private final CognitoClientCredentialsSecret cognitoClientCredentialsSecret;
    private final String authenticationCookieDomain;

    @Inject
    public CognitoService(
            final CognitoStackOutputs cognitoStackOutputs,
            final CognitoClientCredentialsSecret cognitoClientCredentialsSecret,
            @AuthenticationCookieDomain final String authenticationCookieDomain
    ) {
        this.cognitoStackOutputs = cognitoStackOutputs;
        this.cognitoClientCredentialsSecret = cognitoClientCredentialsSecret;
        this.authenticationCookieDomain = authenticationCookieDomain;
    }

    @Override
    public <Req> boolean allow(Req request, User ignoredUser) {
        return request instanceof ProcessAuthCodeRequest ||
                request instanceof GetSignInUrlRequest ||
                request instanceof GetCurrentUserRequest;
    }

    @Override
    public void getSignInUrl(GetSignInUrlRequest request, StreamObserver<GetSignInUrlResponse> responseObserver) {
        responseObserver.onNext(GetSignInUrlResponse.newBuilder().setSignInUrl(
                cognitoStackOutputs.cognitoUserPoolDomainSignInUrl()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getCurrentUser(GetCurrentUserRequest request, StreamObserver<GetCurrentUserResponse> responseObserver) {
        User user = UserContext.getCurrentUser();
        responseObserver.onNext(
                GetCurrentUserResponse.newBuilder()
                        .setDisplayName(user.getDisplayName())
                        .setIsAuthenticated(user.isAuthenticated())
                        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void processAuthCode(ProcessAuthCodeRequest request, StreamObserver<ProcessAuthCodeResponse> responseObserver) {
        try {
            Tokens tokens = getTokens(request.getAuthCode(), responseObserver);

            Context.current().withValue(
                    CookieServerInterceptor.COOKIE_KEY,
                    String.join("|",
                            "logosIdToken=%s; Path=/; Domain=%s; Secure; HttpOnly; SameSite=None; Max-Age=28800".formatted(tokens.id_token, this.authenticationCookieDomain)//,
                    )
            ).wrap(() -> {
                responseObserver.onNext(
                        ProcessAuthCodeResponse.newBuilder()
                                .setExpiresIn(tokens.expires_in)
                                .build());
                responseObserver.onCompleted();
            }).run();
        } catch (IOException e) {
            onFailedRequest(responseObserver, TOKEN_REQUEST_FAILURE_MSG, e);
        }
    }

    private Tokens getTokens(String authCode, StreamObserver<ProcessAuthCodeResponse> responseObserver) throws IOException {
        HttpPost tokenRequest = new HttpPost();
        tokenRequest.setURI(URI.create(cognitoStackOutputs.cognitoUserPoolDomainBaseUrl() + "/oauth2/token"));
        tokenRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
        tokenRequest.setEntity(new UrlEncodedFormEntity(List.of(
                new BasicNameValuePair("grant_type", "authorization_code"),
                new BasicNameValuePair("code", authCode),
                new BasicNameValuePair("client_id", cognitoClientCredentialsSecret.clientId()),
                new BasicNameValuePair("client_secret", cognitoClientCredentialsSecret.clientSecret()),
                new BasicNameValuePair("redirect_uri", cognitoStackOutputs.cognitoUserPoolDomainRedirectUrl())
        )));
        tokenRequest.setConfig(RequestConfig.custom()
                .setConnectTimeout(10 * 1000)
                .setConnectionRequestTimeout(10 * 1000)
                .build());

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse tokenResponse = client.execute(tokenRequest)) {
            if (tokenResponse.getStatusLine().getStatusCode() != HttpStatusCode.OK) {
                throw new IOException(EntityUtils.toString(tokenResponse.getEntity()));
            }

            return new Gson().fromJson(EntityUtils.toString(tokenResponse.getEntity()), Tokens.class);
        }
    }

    private void runWithContext(Context context, Runnable runnable) {
        Context previousContext = context.attach();
        try {
            runnable.run();
        } finally {
            context.detach(previousContext);
        }
    }

    @Override
    public void logOut(LogOutRequest request, StreamObserver<LogOutResponse> responseObserver) {
        Context currentContext = Context.current().withValue(
                CookieServerInterceptor.COOKIE_KEY,
                "logosIdToken=deleted; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT"
        );

        Context previousContext = currentContext.attach();
        try {
            responseObserver.onNext(LogOutResponse.newBuilder().build());
            responseObserver.onCompleted();
        } finally {
            currentContext.detach(previousContext);
        }
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
}