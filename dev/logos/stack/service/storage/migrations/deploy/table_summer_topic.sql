-- Deploy logos:table_summer_topic to pg
-- requires: schema_summer
-- requires: table_summer_entry

BEGIN;

create table summer.topic (
    id uuid primary key default uuid_generate_v4(),
    name text not null,
    summary text,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    parent_id uuid references summer.topic(id) on delete cascade,
    tags text[] not null default '{}'
);

grant select, insert, update, delete on summer.topic to storage;

COMMIT;
