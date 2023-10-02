package app.digits.storage;

import app.digits.storage.digits.PhoneNumber;
import dev.logos.stack.service.storage.TableStorage;

import java.util.UUID;

import static app.digits.storage.Digits.phoneNumber;

public class PhoneNumberStorage extends TableStorage<PhoneNumber, UUID> {

    public PhoneNumberStorage() {
        super(phoneNumber, PhoneNumber.class, UUID.class);
    }
}