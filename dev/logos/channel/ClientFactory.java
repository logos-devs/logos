package dev.logos.channel;

import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


public class ClientFactory {

    public static <T extends AbstractStub<T>> T createStub(
            Class<T> stubClass,
            ManagedChannel inProcessChannel,
            ManagedChannel networkChannel) {

        ManagedChannel dynamicChannel = createDynamicChannel(inProcessChannel, networkChannel);

        try {
            Method newStubMethod = stubClass.getMethod("newBlockingStub", ManagedChannel.class);
            Object possibleStub = newStubMethod.invoke(null, dynamicChannel);

            if (stubClass.isInstance(possibleStub)) {
                return stubClass.cast(possibleStub);
            } else {
                throw new RuntimeException("Generated stub is not of the expected type");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create stub", e);
        }
    }

    private static ManagedChannel createDynamicChannel(
            ManagedChannel inProcessChannel,
            ManagedChannel networkChannel) {
        return (ManagedChannel) Proxy.newProxyInstance(
                ManagedChannel.class.getClassLoader(),
                new Class[]{ManagedChannel.class},
                new DynamicChannelInvocationHandler(inProcessChannel, networkChannel)
        );
    }

    private record DynamicChannelInvocationHandler(
            ManagedChannel inProcessChannel,
            ManagedChannel networkChannel
    ) implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("shutdown".equals(method.getName())) {
                inProcessChannel.shutdown();
                networkChannel.shutdown();
                return null;
            }

            ManagedChannel channelToUse = shouldUseInProcess() ? inProcessChannel : networkChannel;
            return method.invoke(channelToUse, args);
        }

        private boolean shouldUseInProcess() {
            return true;
        }
    }
}
