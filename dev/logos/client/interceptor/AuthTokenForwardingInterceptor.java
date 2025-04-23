package dev.logos.client.interceptor;

import com.google.inject.Inject;
import dev.logos.user.User;
import dev.logos.user.UserContext;
import io.grpc.*;

import java.util.Optional;
import java.util.logging.Logger;

public class AuthTokenForwardingInterceptor implements ClientInterceptor {
    private final Logger logger;

    @Inject
    public AuthTokenForwardingInterceptor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        Optional<User> user = UserContext.getCurrentUser();

        if (user.isPresent()) {
            callOptions = callOptions.withCallCredentials(new AuthTokenCallCredentials(user.get().getToken()));
        }

        return next.newCall(method, callOptions);
    }
}
