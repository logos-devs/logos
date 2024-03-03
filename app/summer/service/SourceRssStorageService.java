package app.summer.service;

import app.summer.storage.summer.CreateSourceRssRequest;
import app.summer.storage.summer.ListSourceRssRequest;
import app.summer.storage.summer.SourceRssStorageServiceBase;
import dev.logos.service.storage.pg.Select;
import dev.logos.user.User;

import static app.summer.storage.Summer.sourceRss;
import static dev.logos.service.storage.pg.Column.STAR;
import static dev.logos.service.storage.pg.Select.select;


public class SourceRssStorageService extends SourceRssStorageServiceBase {
    @Override
    public <Req> boolean allow(Req request, User user) {
        if (request instanceof ListSourceRssRequest) { return true; }
        else if (request instanceof CreateSourceRssRequest) { return user.isAuthenticated(); }
        return false;
    }

    @Override
    public Select.Builder query(ListSourceRssRequest request) {
        return select(STAR).from(sourceRss);
    }
}
