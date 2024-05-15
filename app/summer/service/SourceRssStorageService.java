package app.summer.service;

import app.summer.storage.summer.CreateSourceRssRequest;
import app.summer.storage.summer.CreateSourceRssResponse;
import app.summer.storage.summer.ListSourceRssRequest;
import app.summer.storage.summer.SourceRssStorageServiceBase;
import com.google.protobuf.GeneratedMessageV3;
import dev.logos.service.storage.pg.Select;
import dev.logos.user.User;

import java.util.UUID;

import static app.summer.storage.Summer.SourceRss.*;
import static app.summer.storage.Summer.sourceRss;
import static dev.logos.service.storage.pg.Select.select;


public class SourceRssStorageService extends SourceRssStorageServiceBase {
    @Override
    public <Req> boolean allow(Req request, User user) {
        return user.isAuthenticated();
    }

    @Override
    public Select.Builder query(ListSourceRssRequest request) {
        return select(id, name, url, faviconUrl).from(sourceRss);
    }

    @Override
    public <Req extends GeneratedMessageV3, Resp extends GeneratedMessageV3> Resp response(UUID id, Req request) {
        if (request instanceof CreateSourceRssRequest) {
            CreateSourceRssResponse response =
                CreateSourceRssResponse.newBuilder().build();
            return (Resp) response;
        }
        return null;
    }
}