package app.auth.k8s.interceptor.serviceaccount;

import app.auth.k8s.machine.ServiceAccountMachine;
import com.google.inject.Inject;
import dev.logos.auth.machine.Machine;
import io.grpc.*;

import java.util.Optional;

import static dev.logos.auth.machine.MachineContext.MACHINE_CONTEXT_KEY;

public class ServiceAccountInterceptor implements ServerInterceptor {
    public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Inject
    public ServiceAccountInterceptor() {
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, final Metadata requestHeaders, ServerCallHandler<ReqT, RespT> next) {
        Context ctx = Context.current();
        Metadata internalRequestHeaders = new Metadata();
        internalRequestHeaders.merge(requestHeaders);

        Optional<String> serviceAccountToken = Optional.ofNullable(requestHeaders.get(AUTHORIZATION_METADATA_KEY))
                                                       .filter(authHeader -> authHeader.startsWith("Bearer "))
                                                       .map(authHeader -> authHeader.substring("Bearer ".length()));

        if (serviceAccountToken.isPresent()) {
            String token = serviceAccountToken.get();
            Optional<ServiceAccountMachine> machine = ServiceAccountMachine.fromToken(token);
            if (machine.isPresent()) {
                ctx = ctx.withValue(MACHINE_CONTEXT_KEY, machine.get());
            }
        }

        return Contexts.interceptCall(ctx, call, internalRequestHeaders, next);
    }
}
