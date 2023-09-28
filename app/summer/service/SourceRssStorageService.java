package app.summer.service;

import app.summer.storage.summer.ListSourceRssRequest;
import app.summer.storage.summer.SourceRssStorageServiceBase;
import dev.logos.stack.service.storage.pg.Select;

import static app.summer.storage.Summer.SourceRss.sourceRss;

public class SourceRssStorageService extends SourceRssStorageServiceBase {

    @Override
    public Select.Builder listQuery(ListSourceRssRequest request) {
        return Select.builder().from(sourceRss);
    }
}