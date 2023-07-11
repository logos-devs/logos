package app.digits.service;

import app.digits.storage.PhoneNumberStorage;
import app.digits.storage.digits.ListPhoneNumberRequest;
import app.digits.storage.digits.ListPhoneNumberResponse;
import app.digits.storage.digits.PhoneNumberStorageServiceGrpc;
import com.google.inject.Inject;
import dev.logos.stack.service.storage.exceptions.EntityReadException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class PhoneNumberStorageService extends PhoneNumberStorageServiceGrpc.PhoneNumberStorageServiceImplBase {

    private final PhoneNumberStorage phoneNumberStorage;

    @Inject
    public PhoneNumberStorageService(PhoneNumberStorage phoneNumberStorage) {
        this.phoneNumberStorage = phoneNumberStorage;
    }

    @Override
    public void listPhoneNumber(
        ListPhoneNumberRequest request,
        StreamObserver<ListPhoneNumberResponse> responseObserver
    ) {
        try {
            responseObserver.onNext(
                ListPhoneNumberResponse
                    .newBuilder()
                    .addAllResults(phoneNumberStorage.list().toList()) // TODO: stream to client instead?
                    .build());
        } catch (EntityReadException e) {
            responseObserver.onError(
                Status.UNAVAILABLE
                    .withDescription("Lookup failed.")
                    .asRuntimeException());
        }
        responseObserver.onCompleted();
    }
}