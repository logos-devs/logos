create or replace function migrations.apply_summer_00003_table_summer_entry() returns void as
$mig$
begin
    create table summer.entry
    (
        id uuid primary key default uuid_generate_v4(),
        name text not null,
        body text not null,
        link_url text,
        image_url text,
        created_at timestamp with time zone not null default now(),
        published_at timestamp with time zone not null,
        updated_at timestamp with time zone not null default now(),
        parent_id uuid references summer.entry (id) on delete cascade,
        tags text[] not null default '{}',
        source_rss_id uuid references summer.source_rss (id)
    );

    grant select, insert, update, delete on summer.entry to storage;

    create or replace function tests.role_storage_can_select_from_summer_entry() returns void as
    $$
    begin
        set role to storage;
        perform from summer.entry limit 0;
    end;
    $$ language plpgsql;

end;
$mig$ language plpgsql;


create or replace function migrations.revert_summer_00003_table_summer_entry() returns void as
$$
begin
    drop function tests.role_storage_can_select_from_summer_entry();
    drop table summer.entry;
end;
$$ language plpgsql;


select
from migrations.apply('summer',
                      3,
                      'table_summer_entry',
                      'Create the summer.entry table.');