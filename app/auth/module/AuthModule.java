package app.auth.module;

import app.auth.service.AuthService;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.grpc.BindableService;

public class AuthModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), BindableService.class)
                   .addBinding().to(AuthService.class);
       
        super.configure();
    }
}