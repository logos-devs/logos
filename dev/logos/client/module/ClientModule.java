package dev.logos.client.module;

import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.ProvidesIntoOptional;
import dev.logos.app.AppModule;
import dev.logos.app.register.registerModule;
import dev.logos.client.interceptor.AuthTokenForwardingInterceptor;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;

import java.util.Set;

@registerModule
public class ClientModule extends AppModule {
    @Override
    protected void configure() {
        Multibinder
                .newSetBinder(binder(), ClientInterceptor.class)
                .addBinding().to(AuthTokenForwardingInterceptor.class);

    }

    @ProvidesIntoOptional(ProvidesIntoOptional.Type.DEFAULT)
    public ManagedChannel provideManagedChannel(Set<ClientInterceptor> clientInterceptors) {
        /* TODO select in-process vs network channel with offload logic */
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName("logos-in-process");

        for (ClientInterceptor interceptor : clientInterceptors) {
            channelBuilder.intercept(interceptor);
        }

        return channelBuilder.build();
    }
}