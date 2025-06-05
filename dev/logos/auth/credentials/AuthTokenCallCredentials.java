package dev.logos.auth.credentials;

import dev.logos.auth.user.UserContext;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

import java.util.concurrent.Executor;

public class AuthTokenCallCredentials extends CallCredentials {
    private final String authToken;

    public AuthTokenCallCredentials(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
        appExecutor.execute(() -> {
            Metadata headers = new Metadata();
            headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + authToken);
            applier.apply(headers);
        });
    }

    public static AuthTokenCallCredentials userCredentials() {
        return new AuthTokenCallCredentials(
                UserContext.getCurrentUser().orElseThrow().getToken());
    }
}
