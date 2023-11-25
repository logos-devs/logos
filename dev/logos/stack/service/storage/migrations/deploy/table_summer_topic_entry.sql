-- Deploy logos:table_summer_topic_entry to pg
-- requires: table_summer_topic

BEGIN;

create table summer.topic_entry (
    id uuid primary key default uuid_generate_v4(),
    topic_id uuid references summer.topic(id) on delete cascade,
    entry_id uuid references summer.entry(id) on delete cascade
);

grant select, insert, update, delete on summer.topic_entry to storage;

COMMIT;
