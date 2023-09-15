package app.digits.service;

import app.digits.storage.PhoneNumberStorage;
import app.digits.storage.digits.ListPhoneNumberRequest;
import app.digits.storage.digits.ListPhoneNumberResponse;
import app.digits.storage.digits.PhoneNumber;
import app.digits.storage.digits.PhoneNumberStorageServiceGrpc;
import com.google.inject.Inject;
import dev.logos.stack.service.storage.exceptions.EntityReadException;
import dev.logos.stack.service.storage.pg.Select;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.stream.Stream;

import static app.digits.storage.Digits.PhoneNumber.phoneNumber;

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
        try (Stream<PhoneNumber> phoneNumberListStream = phoneNumberStorage.query(
                Select.builder().from(phoneNumber)
        )) {
            responseObserver.onNext(
                    ListPhoneNumberResponse
                            .newBuilder()
                            .addAllResults(phoneNumberListStream.toList())
                            .build());
            responseObserver.onCompleted();
        } catch (EntityReadException e) {
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Lookup failed.")
                            .asRuntimeException());
        }
    }
}