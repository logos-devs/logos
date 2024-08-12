package dev.logos.service.storage.validator;

public class Validator {
    private boolean valid;

    public Validator () {
        this.valid = true;
    }

    public Validator require(boolean condition, String message) {
        if (!condition) {
            this.valid = false;
        }
        return this;
    }

    public boolean isValid() {
        return this.valid;
    }
}
