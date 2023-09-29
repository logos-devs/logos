package app.summer.storage;

import app.summer.storage.summer.SourceRss;
import dev.logos.stack.service.storage.TableStorage;

import java.util.UUID;

import static app.summer.storage.Summer.SourceRss.sourceRss;

public class SourceRssStorage extends TableStorage<SourceRss, UUID> {

    public SourceRssStorage() {
        super(sourceRss, SourceRss.class, UUID.class);
    }
}
