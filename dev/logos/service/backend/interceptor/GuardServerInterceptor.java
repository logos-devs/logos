package dev.logos.service.backend.interceptor;

import com.google.inject.Inject;
import dev.logos.service.Service;
import io.grpc.*;

import java.util.Map;
import java.util.Optional;

public class GuardServerInterceptor implements ServerInterceptor {
    private final Map<String, Service> services;

    @Inject
    GuardServerInterceptor(Map<String, Service> services) {
        this.services = services;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Service service = services.get(call.getMethodDescriptor().getServiceName());
        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
            @Override
            public void onMessage(ReqT message) {
                Optional<Status> guardResult = service.guard(message);
                if (guardResult.isPresent()) {
                    call.close(guardResult.get(), new Metadata());
                    return;
                }
                super.onMessage(message);
            }
        };
    }
}