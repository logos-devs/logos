package dev.logos.service.storage.validator;

import java.util.ArrayList;
import java.util.List;

public class Validator {
    private boolean valid;
    private final List<String> errorMessages;

    public Validator () {
        this.valid = true;
        this.errorMessages = new ArrayList<>();
    }

    public Validator require(boolean condition, String message) {
        if (!condition) {
            this.valid = false;
            this.errorMessages.add(message);
        }
        return this;
    }

    public boolean isValid() {
        return this.valid;
    }

    public List<String> getErrorMessages() {
        return this.errorMessages;
    }
}
