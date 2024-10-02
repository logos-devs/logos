package dev.logos.service;

import dev.logos.service.storage.validator.Validator;
import dev.logos.user.User;
import dev.logos.user.UserContext;
import io.grpc.BindableService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.logging.Level;

public interface Service extends BindableService {
    Logger logger = LoggerFactory.getLogger(Service.class);

    default <Req> boolean allow(Req request, User user) { return false; }

    default <Req> void validate(Req request, Validator validator) { }

    default void onFailedRequest(
            StreamObserver<?> responseObserver,
            String msg,
            Object obj
    ) {

        logger.atError().log(msg, obj);
        responseObserver.onError(
                Status.INVALID_ARGUMENT
                        .withDescription(msg)
                        .asRuntimeException());
    }

    default <Req> Optional<Status> guard(Req request) {
        if (!allow(request, UserContext.getCurrentUser())) {
            return Optional.of(Status.PERMISSION_DENIED.withDescription("x"));
        }

        Validator validator = new Validator();
        validate(request, validator);

        if (!validator.isValid()) {
            // TODO attach validation failures to status
            return Optional.of(Status.INVALID_ARGUMENT);
        }

        return Optional.empty();
    }
}