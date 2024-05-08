create or replace function migrations.apply_summer_00004_table_summer_topic() returns void as
$mig$
begin

    create table summer.topic
    (
        id uuid primary key default uuid_generate_v4(),
        name text not null,
        summary text,
        created_at timestamp with time zone not null default now(),
        updated_at timestamp with time zone not null default now(),
        parent_id uuid references summer.topic (id) on delete cascade,
        tags text[] not null default '{}'
    );

    create table summer.topic_entry
    (
        id uuid primary key default uuid_generate_v4(),
        topic_id uuid references summer.topic (id) on delete cascade,
        entry_id uuid references summer.entry (id) on delete cascade
    );

    grant select, insert, update, delete on summer.topic to storage;
    grant select, insert, update, delete on summer.topic_entry to storage;

    create or replace function tests.role_storage_can_select_from_summer_topic() returns void as
    $$
    begin
        set role to storage;
        perform from summer.topic limit 0;
    end;
    $$ language plpgsql;

    create or replace function tests.role_storage_can_select_from_summer_topic_entry() returns void as
    $$
    begin
        set role to storage;
        perform from summer.topic_entry limit 0;
    end;
    $$ language plpgsql;

end;
$mig$ language plpgsql;


create or replace function migrations.revert_summer_00004_table_summer_topic() returns void as
$$
begin
    drop table summer.topic_entry;
    drop table summer.topic;
end;
$$ language plpgsql;


select
from migrations.apply('summer',
                      4,
                      'table_summer_topic',
                      'Create the summer.summer_topic table.');