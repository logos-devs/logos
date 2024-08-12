package dev.logos.service.storage.exceptions;

import java.io.IOException;

public class EntityReadException extends IOException {
    public EntityReadException() {
        super();
    }

    public EntityReadException(String message) {
        super(message);
    }

    public EntityReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityReadException(Throwable cause) {
        super(cause);
    }
}
