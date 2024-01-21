package app.digits.service;

import app.digits.storage.digits.ListPhoneNumberRequest;
import app.digits.storage.digits.PhoneNumberStorageServiceBase;
import dev.logos.service.storage.pg.Select;

import static app.digits.storage.Digits.phoneNumber;
import static dev.logos.service.storage.pg.Select.select;

public class PhoneNumberStorageService extends PhoneNumberStorageServiceBase {
    @Override
    public Select.Builder query(ListPhoneNumberRequest request) {
        return select().from(phoneNumber);
    }
}
