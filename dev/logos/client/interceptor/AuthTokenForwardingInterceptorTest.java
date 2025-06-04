package dev.logos.client.interceptor;

import dev.logos.auth.credentials.AuthTokenCallCredentials;
import dev.logos.auth.user.User;
import dev.logos.auth.user.UserContext;
import io.grpc.*;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthTokenForwardingInterceptorTest {
    private static final MethodDescriptor<Void, Void> DUMMY_METHOD =
            MethodDescriptor.<Void, Void>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName("test/method")
                    .setRequestMarshaller(new VoidMarshaller())
                    .setResponseMarshaller(new VoidMarshaller())
                    .build();

    private static class VoidMarshaller implements MethodDescriptor.Marshaller<Void> {
        @Override
        public InputStream stream(Void value) {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public Void parse(InputStream stream) {
            return null;
        }
    }

    private static class DummyUser extends User {
        private final String token;
        DummyUser(String token) { this.token = token; }
        @Override public String getDisplayName() { return "dummy"; }
        @Override public boolean isAuthenticated() { return true; }
        @Override public String getId() { return "id"; }
        @Override public String getToken() { return token; }
    }

    @Test
    public void interceptCall_addsCredentialsWhenUserPresent() {
        Channel channel = mock(Channel.class);
        when(channel.newCall(any(), any())).thenReturn(mock(ClientCall.class));
        AuthTokenForwardingInterceptor interceptor = new AuthTokenForwardingInterceptor(Logger.getLogger("test"));

        Context ctx = Context.current().withValue(UserContext.USER_CONTEXT_KEY, new DummyUser("token"));
        ctx.run(() -> interceptor.interceptCall(DUMMY_METHOD, CallOptions.DEFAULT, channel));

        ArgumentCaptor<CallOptions> captor = ArgumentCaptor.forClass(CallOptions.class);
        verify(channel).newCall(eq(DUMMY_METHOD), captor.capture());
        assertTrue(captor.getValue().getCredentials() instanceof AuthTokenCallCredentials);
    }

    @Test
    public void interceptCall_withoutUserLeavesCredentials() {
        Channel channel = mock(Channel.class);
        when(channel.newCall(any(), any())).thenReturn(mock(ClientCall.class));
        AuthTokenForwardingInterceptor interceptor = new AuthTokenForwardingInterceptor(Logger.getLogger("test"));

        interceptor.interceptCall(DUMMY_METHOD, CallOptions.DEFAULT, channel);

        ArgumentCaptor<CallOptions> captor = ArgumentCaptor.forClass(CallOptions.class);
        verify(channel).newCall(eq(DUMMY_METHOD), captor.capture());
        assertNull(captor.getValue().getCredentials());
    }
}
