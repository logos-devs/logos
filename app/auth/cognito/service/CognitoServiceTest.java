package app.auth.cognito.service;

import app.auth.cognito.module.data.CognitoClientCredentialsSecret;
import app.auth.cognito.module.data.CognitoStackOutputs;
import app.auth.proto.cognito.GetCurrentUserRequest;
import app.auth.proto.cognito.GetCurrentUserResponse;
import dev.logos.auth.user.User;
import dev.logos.auth.user.UserContext;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CognitoServiceTest {

    private CognitoService createService() {
        return new CognitoService(
                mock(CognitoStackOutputs.class),
                mock(CognitoClientCredentialsSecret.class),
                "example.com");
    }

    @Test
    public void getCurrentUser_returnsAuthenticatedUser() {
        CognitoService service = createService();

        User user = mock(User.class);
        when(user.getDisplayName()).thenReturn("Test User");
        when(user.isAuthenticated()).thenReturn(true);

        AtomicReference<GetCurrentUserResponse> result = new AtomicReference<>();
        StreamObserver<GetCurrentUserResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(GetCurrentUserResponse value) {
                result.set(value);
            }

            @Override
            public void onError(Throwable t) {
                fail(t.getMessage());
            }

            @Override
            public void onCompleted() {
            }
        };

        Context.current().withValue(UserContext.USER_CONTEXT_KEY, user)
               .wrap(() -> service.getCurrentUser(
                       GetCurrentUserRequest.newBuilder().build(), observer))
               .run();

        assertNotNull(result.get());
        assertEquals("Test User", result.get().getDisplayName());
        assertTrue(result.get().getIsAuthenticated());
    }

    @Test
    public void getCurrentUser_returnsAnonymousWhenNoUser() {
        CognitoService service = createService();

        AtomicReference<GetCurrentUserResponse> result = new AtomicReference<>();
        StreamObserver<GetCurrentUserResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(GetCurrentUserResponse value) {
                result.set(value);
            }

            @Override
            public void onError(Throwable t) {
                fail(t.getMessage());
            }

            @Override
            public void onCompleted() {
            }
        };

        service.getCurrentUser(GetCurrentUserRequest.newBuilder().build(), observer);

        assertNotNull(result.get());
        assertEquals("Anonymous", result.get().getDisplayName());
        assertFalse(result.get().getIsAuthenticated());
    }
}
