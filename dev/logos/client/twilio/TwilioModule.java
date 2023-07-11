package dev.logos.client.twilio;

import com.google.inject.AbstractModule;
import com.twilio.http.TwilioRestClient;


public class TwilioModule extends AbstractModule {

    public TwilioModule() {
        super();
    }

    @Override
    protected void configure() {
        bind(TwilioRestClient.class).toInstance(
            new TwilioRestClient.Builder(
                System.getenv("TWILIO_SID"),
                System.getenv("TWILIO_AUTH_TOKEN")
            ).build()
        );
        super.configure();
    }
}