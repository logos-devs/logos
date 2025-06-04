package dev.logos.service.backend.server;

import dev.logos.service.Service;
import io.grpc.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class GuardServerInterceptorTest {

    private static MethodDescriptor<String, String> descriptor() {
        MethodDescriptor.Marshaller<String> marshaller = new MethodDescriptor.Marshaller<>() {
            @Override
            public InputStream stream(String value) {
                return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public String parse(InputStream stream) {
                try {
                    return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return MethodDescriptor.<String, String>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("svc/method")
                .setRequestMarshaller(marshaller)
                .setResponseMarshaller(marshaller)
                .build();
    }

    @Test
    public void guardReturnsStatus_closesCall() {
        Service service = mock(Service.class);
        ServerCall<String, String> call = mock(ServerCall.class);
        ServerCallHandler<String, String> handler = mock(ServerCallHandler.class);
        ServerCall.Listener<String> delegate = mock(ServerCall.Listener.class);

        Metadata headers = new Metadata();
        when(call.getMethodDescriptor()).thenReturn(descriptor());
        when(handler.startCall(call, headers)).thenReturn(delegate);
        when(service.guard("msg")).thenReturn(Optional.of(Status.ABORTED));

        GuardServerInterceptor interceptor = new GuardServerInterceptor(Map.of("svc", service));
        ServerCall.Listener<String> listener = interceptor.interceptCall(call, headers, handler);

        listener.onMessage("msg");
        listener.onHalfClose();
        listener.onCancel();
        listener.onComplete();
        listener.onReady();

        verify(call).close(eq(Status.ABORTED), any(Metadata.class));
        verify(delegate, never()).onMessage(any());
        verify(delegate, never()).onHalfClose();
        verify(delegate, never()).onCancel();
        verify(delegate, never()).onComplete();
        verify(delegate, never()).onReady();
    }

    @Test
    public void guardReturnsEmpty_forwardsMessage() {
        Service service = mock(Service.class);
        ServerCall<String, String> call = mock(ServerCall.class);
        ServerCallHandler<String, String> handler = mock(ServerCallHandler.class);
        ServerCall.Listener<String> delegate = mock(ServerCall.Listener.class);

        Metadata headers = new Metadata();
        when(call.getMethodDescriptor()).thenReturn(descriptor());
        when(handler.startCall(call, headers)).thenReturn(delegate);
        when(service.guard("msg")).thenReturn(Optional.empty());

        GuardServerInterceptor interceptor = new GuardServerInterceptor(Map.of("svc", service));
        ServerCall.Listener<String> listener = interceptor.interceptCall(call, headers, handler);

        listener.onMessage("msg");

        verify(delegate).onMessage("msg");
        verify(call, never()).close(any(), any());
    }
}
