package app.summer.service;

import app.summer.storage.summer.EntryStorageServiceBase;
import app.summer.storage.summer.ListEntryRequest;
import dev.logos.stack.service.storage.pg.Select;

import static app.summer.storage.Summer.Entry.ParentId.parentId;
import static app.summer.storage.Summer.Entry.PublishedAt.publishedAt;
import static app.summer.storage.Summer.Entry.entry;
import static dev.logos.stack.service.storage.pg.Filter.Op.IS_NULL;
import static dev.logos.stack.service.storage.pg.Select.select;
import static dev.logos.stack.service.storage.pg.SortOrder.DESC;


public class EntryStorageService extends EntryStorageServiceBase {
    @Override
    public Select.Builder query(ListEntryRequest request) {
        return select()
                .from(entry)
                .where(parentId, IS_NULL)
                .orderBy(publishedAt, DESC)
                .limit(50L)
                .offset(request.getOffset());
    }
}