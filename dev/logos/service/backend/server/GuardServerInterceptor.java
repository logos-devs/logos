package dev.logos.service.backend.server;

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
        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        String serviceName = call.getMethodDescriptor().getServiceName();
        if (!services.containsKey(serviceName))
            return delegate;

        Service service = services.get(serviceName);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
            private boolean callClosed = false;

            @Override
            public void onMessage(ReqT message) {
                if (!callClosed) {
                    Optional<Status> guardResult = service.guard(message);
                    if (guardResult.isPresent()) {
                        call.close(guardResult.get(), new Metadata());
                        callClosed = true;
                        return;
                    }
                    super.onMessage(message);
                }
            }

            @Override
            public void onHalfClose() {
                if (!callClosed) {
                    super.onHalfClose();
                }
            }

            @Override
            public void onCancel() {
                if (!callClosed) {
                    super.onCancel();
                }
            }

            @Override
            public void onComplete() {
                if (!callClosed) {
                    super.onComplete();
                }
            }

            @Override
            public void onReady() {
                if (!callClosed) {
                    super.onReady();
                }
            }
        };
    }
}