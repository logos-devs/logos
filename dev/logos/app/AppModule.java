package dev.logos.app;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import dev.logos.service.Service;
import io.grpc.*;
import io.grpc.stub.AbstractStub;
import software.amazon.awscdk.Stack;

import java.util.Optional;
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

    @SafeVarargs
    protected final void clients(Function<ManagedChannel, ? extends AbstractStub<?>>... stubFactories) {
        for (Function<ManagedChannel, ? extends AbstractStub<?>> stubFactory : stubFactories) {
            client(stubFactory);
        }
    }

    @SuppressWarnings("unchecked")
    protected void client(Function<ManagedChannel, ? extends AbstractStub<?>> stubFactory) {
        AbstractStub<?> dummyStub = stubFactory.apply(DUMMY_CHANNEL);
        Class<AbstractStub<?>> stubClass = (Class<AbstractStub<?>>) dummyStub.getClass();
        Provider<ManagedChannel> channelProvider = getProvider(ManagedChannel.class);

        // This cast is safe because stubFactory always produces the correct subclass for stubClass
        Provider<? extends AbstractStub<?>> provider = () -> stubFactory.apply(channelProvider.get());

        bind(stubClass).toProvider((Provider) provider);
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