create or replace function migrations.apply_summer_00005_table_summer_source_imap() returns void as
$mig$
begin

    create table summer.source_imap
    (
        id uuid primary key default uuid_generate_v4(),
        address text not null,
        credentials_pubkey text not null,
        created_at timestamp with time zone not null default now(),
        updated_at timestamp with time zone not null default now()
    );

    grant select, insert, update, delete on summer.source_imap to storage;

    create or replace function tests.role_storage_can_select_from_summer_source_imap() returns void as
    $$
    begin
        set role to storage;
        perform from summer.source_imap limit 0;
    end;
    $$ language plpgsql;

    alter table summer.entry
        add column source_imap_id uuid
            references summer.source_imap (id)
                on delete cascade;
end;
$mig$ language plpgsql;


create or replace function migrations.revert_summer_00005_table_summer_source_imap() returns void as
$$
begin
    drop table summer.source_imap;
end;
$$ language plpgsql;



select
from migrations.apply('summer',
                      5,
                      'table_summer_source_imap',
                      'Create the summer.source_imap table.');
