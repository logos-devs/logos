package app.summer.service;

import app.summer.storage.summer.CreateEntryRequest;
import app.summer.storage.summer.Entry;
import app.summer.storage.summer.EntryStorageServiceBase;
import app.summer.storage.summer.ListEntryRequest;
import dev.logos.service.storage.pg.Select;
import dev.logos.service.storage.validator.Validator;
import dev.logos.user.User;

import static app.summer.storage.Summer.Entry.*;
import static app.summer.storage.Summer.entry;
import static dev.logos.service.storage.pg.Filter.Op.IS_NULL;
import static dev.logos.service.storage.pg.Select.select;
import static dev.logos.service.storage.pg.SortOrder.DESC;


public class EntryStorageService extends EntryStorageServiceBase {
    @Override
    public boolean allow(User user, ListEntryRequest request) { return user.isAuthenticated(); }

    @Override
    public boolean allow(User user, CreateEntryRequest request) { return user.isAuthenticated(); }

    @Override
    public void validate(CreateEntryRequest request, Validator validator) {
        Entry entry = request.getEntity();
        validator.require(!entry.getName().isBlank(), "name is required")
                 .require(!entry.getBody().isBlank(), "body is required");
    }

    @Override
    public Select.Builder query(ListEntryRequest request) {
        return select(id, name, body, linkUrl, imageUrl, createdAt, updatedAt, parentId, publishedAt, tags, sourceRssId)
                .from(entry)
                .where(parentId, IS_NULL)
                .orderBy(publishedAt, DESC)
                .limit(50L)
                .offset(request.getOffset());
    }
}