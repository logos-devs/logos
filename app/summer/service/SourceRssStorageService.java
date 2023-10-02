package app.summer.service;

import app.summer.storage.summer.ListSourceRssRequest;
import app.summer.storage.summer.SourceRssStorageServiceBase;
import dev.logos.stack.service.storage.pg.Select;

import static app.summer.storage.Summer.sourceRss;
import static dev.logos.stack.service.storage.pg.Column.STAR;
import static dev.logos.stack.service.storage.pg.Select.select;

public class SourceRssStorageService extends SourceRssStorageServiceBase {
    @Override
    public Select.Builder query(ListSourceRssRequest request) {
        return select(STAR).from(sourceRss);
    }
}