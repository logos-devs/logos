package app.summer.service;

import app.summer.storage.summer.ListSourceRssRequest;
import app.summer.storage.summer.SourceRssStorageServiceBase;
import dev.logos.service.storage.pg.Select;
import dev.logos.user.User;

import static app.summer.storage.Summer.SourceRss.*;
import static app.summer.storage.Summer.sourceRss;
import static dev.logos.service.storage.pg.Select.select;
import static dev.logos.service.storage.pg.SortOrder.ASC;


public class SourceRssStorageService extends SourceRssStorageServiceBase {
    @Override
    public <Req> boolean allow(Req request, User user) {
        return user.isAuthenticated();
    }

    @Override
    public Select.Builder query(ListSourceRssRequest request) {
        return select(id, name, url, faviconUrl).from(sourceRss).orderBy(name, ASC);
    }
}