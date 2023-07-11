package app.digits.service;

import static app.digits.proto.voice.VoiceServiceGrpc.VoiceServiceImplBase;

import app.digits.proto.voice.CallRequest;
import app.digits.proto.voice.CallResponse;
import com.google.inject.Inject;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.type.PhoneNumber;
import com.twilio.type.Twiml;
import io.grpc.stub.StreamObserver;

public class VoiceService extends VoiceServiceImplBase {

    private final TwilioRestClient twilioClient;

    /* TODO Developers should be able to obtain API clients to arbitrary popular 3rd party APIs declaratively using DI.
       TODO They'll supply their API keys via a UI or the command line using kubectl. */
    @Inject
    public VoiceService(TwilioRestClient twilioClient) {
        this.twilioClient = twilioClient;
    }

    @Override
    public void call(
        CallRequest request,
        StreamObserver<CallResponse> responseObserver
    ) {
        Call call = Call.creator(
            new PhoneNumber(request.getToPhoneNumber()),
            new PhoneNumber(request.getFromPhoneNumber()),
            new Twiml(
                /* language=XML */
                "<Response><Dial>832-602-6861</Dial></Response>"
            )).create(this.twilioClient);

        responseObserver.onNext(
            CallResponse.newBuilder().setSid(call.getSid())
                        .build());
        responseObserver.onCompleted();
    }
}
