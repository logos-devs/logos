package dev.logos.service;

import dev.logos.service.storage.validator.Validator;
import io.grpc.BindableService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public interface Service extends BindableService {
    Logger logger = LoggerFactory.getLogger(Service.class);

    default <Req> boolean allow(Req request) {
        return false;
    }

    default <Req> void validate(Req request, Validator validator) {
    }

    default void onFailedRequest(
            StreamObserver<?> responseObserver,
            String msg,
            Throwable e
    ) {
        logger.atError().setCause(e).log(msg);
        responseObserver.onError(
                Status.INVALID_ARGUMENT.withDescription(msg).asException());
    }

    default <Req> Optional<Status> guard(Req request) {
        if (!allow(request)) {
            return Optional.of(Status.PERMISSION_DENIED.withDescription("Permission denied"));
        }

        Validator validator = new Validator();
        validate(request, validator);

        if (!validator.isValid()) {
            return Optional.of(Status.INVALID_ARGUMENT.withDescription(
                    String.join("\n", validator.getErrorMessages())));
        }

        return Optional.empty();
    }
}