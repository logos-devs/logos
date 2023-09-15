-- Deploy logos:table_summer_source_rss to pg
-- requires: schema_summer

BEGIN;

create table summer.source_rss (
    id uuid primary key default uuid_generate_v4(),
    name text not null,
    url text not null,
    image_url text,
    favicon_url text,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    synced_at timestamp with time zone
);

alter table summer.entry add column source_rss_id uuid references summer.source_rss(id);

grant select, update, delete on summer.source_rss to storage;

COMMIT;
