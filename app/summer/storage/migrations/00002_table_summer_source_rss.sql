create or replace function migrations.apply_summer_00002_table_summer_source_rss() returns void as
$mig$
begin

    create table summer.source_rss
    (
        id uuid primary key default uuid_generate_v4(),
        name text not null,
        url text not null,
        image_url text,
        favicon_url text,
        created_at timestamp with time zone not null default now(),
        updated_at timestamp with time zone not null default now(),
        synced_at timestamp with time zone
    );

    grant select, update, delete on summer.source_rss to storage;

    create or replace function tests.role_storage_can_select_from_summer_source_rss() returns void as
    $$
    begin
        set role to storage;
        perform from summer.source_rss limit 0;
    end;
    $$ language plpgsql;

end;
$mig$ language plpgsql;


create or replace function migrations.revert_summer_00002_table_summer_source_rss() returns void as
$$
begin
    drop table summer.source_rss;
end;
$$ language plpgsql;

select
from migrations.apply('summer',
                      2,
                      'table_summer_source_rss',
                      'Create the summer.source_rss table.');