package app.auth.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.util.logging.Logger;

public class AuthInterceptor implements ServerInterceptor {

    private static final Logger logger = Logger.getLogger(AuthInterceptor.class.getName());
    private static final Metadata.Key<String> USER_ID_KEY = Metadata.Key.of("user-id",
                                                                            Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        // Get the authentication token from the Authorization header
        String authToken = headers.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER));

        // Validate the authentication token and extract the user ID
        String userId = validateAuthTokenAndGetUserId(authToken);

        // Add the user ID to the call metadata
        headers.put(USER_ID_KEY, userId);

        logger.info("Added user ID " + userId + " to metadata");

        // Pass control to the next interceptor or to the server handler
        return next.startCall(call, headers);
    }

    private String validateAuthTokenAndGetUserId(String authToken) {
        // TODO: Implement authentication logic here
        // For the purposes of this example, just return a dummy user ID
        return "1234";
    }
}
