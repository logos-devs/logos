package dev.logos.app;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.multibindings.Multibinder;
import dev.logos.service.Service;
import io.grpc.*;
import io.grpc.stub.AbstractStub;
import software.amazon.awscdk.Stack;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public abstract class AppModule extends AbstractModule {
    private static final ManagedChannel DUMMY_CHANNEL = new ManagedChannel() {
        @Override
        public ManagedChannel shutdown() {
            return this;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public ManagedChannel shutdownNow() {
            return this;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
            return null;
        }

        @Override
        public String authority() {
            return null;
        }
    };

    protected void service(Class<? extends Service> serviceClass) {
        Multibinder
                .newSetBinder(binder(), Service.class)
                .addBinding().to(serviceClass);
    }

    @SafeVarargs
    protected final void services(Class<? extends Service>... serviceClasses) {
        for (Class<? extends Service> serviceClass : serviceClasses) {
            service(serviceClass);
        }
    }

    protected void interceptor(Class<? extends ServerInterceptor> interceptorClass) {
        Multibinder
                .newSetBinder(binder(), ServerInterceptor.class)
                .addBinding().to(interceptorClass);
    }

    @SafeVarargs
    protected final void interceptors(Class<? extends ServerInterceptor>... interceptorClasses) {
        for (Class<? extends ServerInterceptor> interceptorClass : interceptorClasses) {
            interceptor(interceptorClass);
        }
    }

    @SuppressWarnings("unchecked")
    protected <S extends AbstractStub<S>> void client(Function<ManagedChannel, S> stubFactory) {
        Class<S> stubClass = (Class<S>) stubFactory.apply(DUMMY_CHANNEL).getClass();
        Provider<ManagedChannel> channelProvider = getProvider(ManagedChannel.class);
        bind(stubClass).toProvider(() -> stubFactory.apply(channelProvider.get()));
    }

    @SafeVarargs
    protected final <S extends AbstractStub<S>> void clients(Function<ManagedChannel, S>... stubFactories) {
        for (Function<ManagedChannel, S> stubFactory : stubFactories) {
            client(stubFactory);
        }
    }

    protected void stack(Class<? extends Stack> stackClass) {
        Multibinder.newSetBinder(binder(), Stack.class).addBinding().to(stackClass);
    }

    @SafeVarargs
    protected final void stacks(Class<? extends Stack>... stackClasses) {
        for (Class<? extends Stack> stackClass : stackClasses) {
            stack(stackClass);
        }
    }
}