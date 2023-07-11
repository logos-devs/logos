package app.digits.module;

import app.digits.service.PhoneNumberStorageService;
import app.digits.service.VoiceService;
import app.digits.storage.PhoneNumberStorage;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import dev.logos.client.twilio.TwilioModule;
import io.grpc.BindableService;

public class DigitsModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new TwilioModule());

        bind(PhoneNumberStorage.class).toInstance(new PhoneNumberStorage());

        Multibinder.newSetBinder(binder(), BindableService.class)
                   .addBinding().to(VoiceService.class);

        Multibinder.newSetBinder(binder(), BindableService.class)
                   .addBinding().to(PhoneNumberStorageService.class);

        super.configure();
    }
}