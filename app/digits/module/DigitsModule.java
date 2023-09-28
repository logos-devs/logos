package app.digits.module;

import app.digits.service.PhoneNumberStorageService;
import app.digits.service.VoiceService;
import app.digits.storage.PhoneNumberStorage;
import app.digits.storage.digits.PhoneNumber;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import dev.logos.client.twilio.TwilioModule;
import dev.logos.stack.service.storage.EntityStorage;
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

        bind(new TypeLiteral<EntityStorage<PhoneNumber>>(){}).toInstance(new PhoneNumberStorage());

        super.configure();
    }
}