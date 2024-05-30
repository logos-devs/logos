package app.summer.service;

import app.summer.storage.summer.ListSourceImapRequest;
import app.summer.storage.summer.SourceImapStorageServiceBase;
import dev.logos.service.storage.pg.Select;
import dev.logos.user.User;

import static app.summer.storage.Summer.SourceImap.address;
import static app.summer.storage.Summer.SourceImap.id;
import static app.summer.storage.Summer.sourceImap;
import static dev.logos.service.storage.pg.Select.select;


public class SourceImapStorageService extends SourceImapStorageServiceBase {
    @Override
    public <Req> boolean allow(Req request, User user) {
        return user.isAuthenticated();
    }

    @Override
    public Select.Builder query(ListSourceImapRequest request) {
        return select(id, address).from(sourceImap);
    }
}
