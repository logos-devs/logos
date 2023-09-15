package app.summer.storage;

import app.summer.storage.summer.SourceRss;
import dev.logos.stack.service.storage.TableStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static app.summer.storage.Summer.SourceRss.sourceRss;
import static app.summer.storage.Summer.SourceRss.toProtobuf;

public class SourceRssStorage extends TableStorage<SourceRss, UUID> {

    public SourceRssStorage() {
        super(sourceRss, SourceRss.class, UUID.class);
    }

    public SourceRss storageToEntity(ResultSet resultSet) throws SQLException {
        return toProtobuf(resultSet);
    }
}
