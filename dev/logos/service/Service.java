package dev.logos.service;

import dev.logos.service.storage.validator.Validator;
import dev.logos.user.User;
import dev.logos.user.UserContext;
import io.grpc.BindableService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public interface Service extends BindableService {
    default boolean allow(Object request, User user) {
        return false;
    }

    default boolean validate(Object request, Validator validator) {
        return true;
    }

    default <T> void error(StreamObserver<T> responseObserver, String description) {
        responseObserver.onError(
                Status.INVALID_ARGUMENT
                        .withDescription(description)
                        .asException());
    }

    default <Req, Resp> boolean guard(Req request, StreamObserver<Resp> responseObserver) {
        if (!allow(request, UserContext.getCurrentUser())) {
            error(responseObserver, "Access denied.");
            return true;
        }

        Validator validator = new Validator();
        if (!validate(request, validator)) {
            error(responseObserver, "Invalid request.");
            return true;
        }

        return false;
    }
}