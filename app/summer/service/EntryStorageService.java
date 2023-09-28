package app.summer.service;

import app.summer.storage.summer.EntryStorageServiceBase;
import app.summer.storage.summer.ListEntryRequest;
import dev.logos.stack.service.storage.pg.OrderBy;
import dev.logos.stack.service.storage.pg.Select;

import java.util.List;

import static app.summer.storage.Summer.Entry.ParentId.parentId;
import static app.summer.storage.Summer.Entry.PublishedAt.publishedAt;
import static app.summer.storage.Summer.Entry.entry;
import static dev.logos.stack.service.storage.pg.Filter.Op.IS_NULL;
import static dev.logos.stack.service.storage.pg.Filter.filter;
import static dev.logos.stack.service.storage.pg.Select.select;
import static dev.logos.stack.service.storage.pg.SortOrder.DESC;


public class EntryStorageService extends EntryStorageServiceBase {
    @Override
    public Select.Builder listQuery(ListEntryRequest request) {
        return select().from(entry)
                .where(List.of(filter()
                                .column(parentId)
                                .op(IS_NULL)
                                .build()))
                .orderBy(OrderBy.builder()
                        .column(publishedAt)
                        .direction(DESC))
                .limit(50);
    }
}