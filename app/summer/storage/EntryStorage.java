package app.summer.storage;

import static app.summer.storage.Summer.Entry.entry;
import static app.summer.storage.Summer.Entry.toProtobuf;

import app.summer.storage.summer.Entry;
import dev.logos.stack.service.storage.TableStorage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class EntryStorage extends TableStorage<Entry, UUID> {

    public EntryStorage() {
        super(entry, Entry.class, UUID.class);
    }

    public Entry storageToEntity(ResultSet resultSet) throws SQLException {
        return toProtobuf(resultSet);
    }
}
