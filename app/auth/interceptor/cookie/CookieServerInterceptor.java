package app.auth.interceptor.cookie;

import com.google.gson.Gson;
import io.grpc.*;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;

import java.util.List;

public class CookieServerInterceptor implements ServerInterceptor {
    public static final Context.Key<List<String>> COOKIE_KEY = Context.key("set-cookies");
    private static final Gson gson = new Gson();

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            final Metadata requestHeaders,
            ServerCallHandler<ReqT, RespT> next
    ) {
        return next.startCall(new SimpleForwardingServerCall<>(call) {
            @Override
            public void sendHeaders(Metadata responseHeaders) {
                List<String> cookies = COOKIE_KEY.get();
                if (cookies != null) {
                    responseHeaders.put(
                            Metadata.Key.of("custom_set_cookies", Metadata.ASCII_STRING_MARSHALLER),
                            gson.toJson(cookies));
                }
                super.sendHeaders(responseHeaders);
            }
        }, requestHeaders);
    }
}