package app.digits.storage;

import static app.digits.storage.Digits.PhoneNumber.phoneNumber;
import static app.digits.storage.Digits.PhoneNumber.toProtobuf;

import app.digits.storage.digits.PhoneNumber;
import dev.logos.stack.service.storage.TableStorage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PhoneNumberStorage extends TableStorage<PhoneNumber, UUID> {

    public PhoneNumberStorage() {
        super(phoneNumber, PhoneNumber.class, UUID.class);
    }

    public PhoneNumber storageToEntity(ResultSet resultSet) throws SQLException {
        return toProtobuf(resultSet);
    }
}
