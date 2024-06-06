package app.digits.module;

import app.digits.service.PhoneNumberStorageService;
import app.digits.service.VoiceService;
import dev.logos.app.AppModule;
import dev.logos.app.register.registerModule;
import dev.logos.client.twilio.TwilioModule;

@registerModule
public class DigitsModule extends AppModule {

    @Override
    protected void configure() {
        install(new TwilioModule());
        addService(VoiceService.class);
        addService(PhoneNumberStorageService.class);
        super.configure();
    }
}