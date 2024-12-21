package app.auth.cognito.interceptor.cookie;

import io.grpc.*;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;

import java.util.Optional;

public class CookieServerInterceptor implements ServerInterceptor {
    public static final Context.Key<String> COOKIE_KEY = Context.key("set-cookies");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            final Metadata requestHeaders,
            ServerCallHandler<ReqT, RespT> next
    ) {
        return next.startCall(new SimpleForwardingServerCall<>(call) {
            @Override
            public void sendHeaders(Metadata responseHeaders) {
                Optional.ofNullable(COOKIE_KEY.get()).ifPresent(
                        cookies -> responseHeaders.put(
                                Metadata.Key.of("logos-set-cookies", Metadata.ASCII_STRING_MARSHALLER),
                                cookies));
                super.sendHeaders(responseHeaders);
            }
        }, requestHeaders);
    }
}