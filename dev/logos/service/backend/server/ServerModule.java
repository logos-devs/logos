package dev.logos.service.backend.server;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import dev.logos.service.Service;
import dev.logos.service.backend.interceptor.GuardServerInterceptor;
import dev.logos.user.NotAuthenticated;
import dev.logos.user.User;
import dev.logos.user.UserContext;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

class AuthTokenCallCredentials extends CallCredentials {
    private final String authToken;

    public AuthTokenCallCredentials(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
        appExecutor.execute(() -> {
            try {
                Metadata headers = new Metadata();
                headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + authToken);
                applier.apply(headers);
            } catch (Throwable e) {
                applier.fail(Status.UNAUTHENTICATED.withCause(e));
            }
        });
    }

    @Override
    public void thisUsesUnstableApi() {
        // No-op
    }
}

class AuthTokenForwardingInterceptor implements ClientInterceptor {
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

public class ServerModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder
                .newSetBinder(binder(), ServerInterceptor.class)
                .addBinding().to(GuardServerInterceptor.class);

        Multibinder
                .newSetBinder(binder(), ClientInterceptor.class)
                .addBinding().to(AuthTokenForwardingInterceptor.class);
    }

    @Provides
    public ManagedChannel provideManagedChannel(Set<ClientInterceptor> clientInterceptors) {
        /* TODO select in-process vs network channel with offload logic */
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName("logos-in-process");

        for (ClientInterceptor interceptor : clientInterceptors) {
            channelBuilder.intercept(interceptor);
        }

        return channelBuilder.build();
    }

    @Provides
    public Map<String, Service> provideServiceMap(Set<Service> services) {
        Map<String, Service> serviceMap = new HashMap<>();

        for (Service service : services) {
            serviceMap.put(service.bindService().getServiceDescriptor().getName(), service);
        }

        return serviceMap;
    }
}
