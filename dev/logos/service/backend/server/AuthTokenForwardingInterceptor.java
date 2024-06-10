package dev.logos.service.backend.server;

import com.google.inject.Inject;
import dev.logos.user.NotAuthenticated;
import dev.logos.user.User;
import dev.logos.user.UserContext;
import io.grpc.*;

import java.util.logging.Logger;

public class AuthTokenForwardingInterceptor implements ClientInterceptor {
    private final Logger logger;

    @Inject
    public AuthTokenForwardingInterceptor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        User user = UserContext.getCurrentUser();

        if (user.isAuthenticated()) {
            try {
                callOptions = callOptions.withCallCredentials(new AuthTokenCallCredentials(user.getToken()));
            } catch (NotAuthenticated ignored) {
                logger.severe("User.isAuthenticated() returned true but user.getToken() threw NotAuthenticated");
            }
        }

        return next.newCall(method, callOptions);
    }
}
