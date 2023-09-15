-- Deploy logos:table_summer_entry to pg
-- requires: schema_summer

BEGIN;

create table summer.entry (
    id uuid primary key default uuid_generate_v4(),
    name text not null,
    body text not null,
    link_url text,
    image_url text,
    created_at timestamp with time zone not null default now(),
    published_at timestamp with time zone not null,
    updated_at timestamp with time zone not null default now(),
    parent_id uuid references summer.entry(id) on delete cascade,
    tags text[] not null default '{}'
);

grant select, insert, update, delete on summer.entry to storage;

COMMIT;
