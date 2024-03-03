package dev.logos.service;

import dev.logos.service.storage.validator.Validator;
import dev.logos.user.User;
import dev.logos.user.UserContext;
import io.grpc.BindableService;
import io.grpc.Status;

import java.util.Optional;

public interface Service extends BindableService {
    default <Req> boolean allow(Req request, User user) { return false; }

    default <Req> void validate(Req request, Validator validator) { }

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