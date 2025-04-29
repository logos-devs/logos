package app.auth.k8s.module;

import app.auth.k8s.interceptor.serviceaccount.ServiceAccountInterceptor;
import app.auth.k8s.machine.ServiceAccountMachine;
import com.google.inject.multibindings.ProvidesIntoOptional;
import dev.logos.app.AppModule;
import dev.logos.app.register.registerModule;
import dev.logos.auth.machine.annotation.MachineScoped;
import io.grpc.CallCredentials;

@registerModule
public class K8sModule extends AppModule {
    @Override
    protected void configure() {
        interceptors(ServiceAccountInterceptor.class);
    }

    @MachineScoped
    @ProvidesIntoOptional(ProvidesIntoOptional.Type.DEFAULT)
    CallCredentials provideCallCredentials() {
        return ServiceAccountMachine.self().callCredentials();
    }
}
