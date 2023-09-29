package app.summer.storage;

import app.summer.storage.summer.Entry;
import dev.logos.stack.service.storage.TableStorage;

import java.util.UUID;

import static app.summer.storage.Summer.Entry.entry;

public class EntryStorage extends TableStorage<Entry, UUID> {

    public EntryStorage() {
        super(entry, Entry.class, UUID.class);
    }
}
