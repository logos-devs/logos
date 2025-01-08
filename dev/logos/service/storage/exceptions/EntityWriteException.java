package dev.logos.service.storage.exceptions;

import java.io.IOException;

public class EntityWriteException extends IOException {
    public EntityWriteException() {
        super();
    }

    public EntityWriteException(String message) {
        super(message);
    }

    public EntityWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityWriteException(Throwable cause) {
        super(cause);
    }
}
