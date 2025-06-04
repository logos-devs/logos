package app.auth.cognito.interceptor.cookie;

import io.grpc.*;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CookieServerInterceptorTest {

    @Test
    public void sendHeaders_addsCookieWhenKeyPresent() {
        CookieServerInterceptor interceptor = new CookieServerInterceptor();
        ServerCall<String, String> call = mock(ServerCall.class);
        Metadata reqHeaders = new Metadata();
        @SuppressWarnings("unchecked")
        ServerCallHandler<String, String> handler = mock(ServerCallHandler.class);
        ServerCall.Listener<String> listener = mock(ServerCall.Listener.class);
        when(handler.startCall(any(), any())).thenReturn(listener);

        Context ctx = Context.current().withValue(CookieServerInterceptor.COOKIE_KEY, "a=b");
        Context prev = ctx.attach();
        try {
            interceptor.interceptCall(call, reqHeaders, handler);

            ArgumentCaptor<ServerCall> callCaptor = ArgumentCaptor.forClass(ServerCall.class);
            verify(handler).startCall(callCaptor.capture(), eq(reqHeaders));
            @SuppressWarnings("unchecked")
            ServerCall<String, String> forwarded = callCaptor.getValue();
            Metadata resp = new Metadata();
            forwarded.sendHeaders(resp);

            assertEquals("a=b", resp.get(Metadata.Key.of("logos-set-cookies", Metadata.ASCII_STRING_MARSHALLER)));
            verify(call).sendHeaders(resp);
        } finally {
            ctx.detach(prev);
        }
    }

    @Test
    public void sendHeaders_noCookieWhenKeyMissing() {
        CookieServerInterceptor interceptor = new CookieServerInterceptor();
        ServerCall<String, String> call = mock(ServerCall.class);
        Metadata reqHeaders = new Metadata();
        @SuppressWarnings("unchecked")
        ServerCallHandler<String, String> handler = mock(ServerCallHandler.class);
        ServerCall.Listener<String> listener = mock(ServerCall.Listener.class);
        when(handler.startCall(any(), any())).thenReturn(listener);

        interceptor.interceptCall(call, reqHeaders, handler);
        ArgumentCaptor<ServerCall> callCaptor = ArgumentCaptor.forClass(ServerCall.class);
        verify(handler).startCall(callCaptor.capture(), eq(reqHeaders));
        @SuppressWarnings("unchecked")
        ServerCall<String, String> forwarded = callCaptor.getValue();
        Metadata resp = new Metadata();
        forwarded.sendHeaders(resp);

        assertNull(resp.get(Metadata.Key.of("logos-set-cookies", Metadata.ASCII_STRING_MARSHALLER)));
        verify(call).sendHeaders(resp);
    }
}
