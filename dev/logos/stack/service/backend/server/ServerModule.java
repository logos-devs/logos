package dev.logos.stack.service.backend.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;

public class ServerModule extends AbstractModule {
    @Provides
    public ManagedChannel provideManagedChannel() {
        /* TODO select channel with offload logic */
        return InProcessChannelBuilder.forName("logos-in-process").build();
    }
}
