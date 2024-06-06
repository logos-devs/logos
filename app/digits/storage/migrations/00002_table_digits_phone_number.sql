create or replace function migrations.apply_digits_00002_table_digits_phone_number() returns void as
$mig$
begin

    create table digits.phone_number
    (
        id uuid primary key default uuid_generate_v4(),
        phone_number text not null
    );

    grant select, insert, update, delete on digits.phone_number to storage;

    create or replace function tests.role_storage_can_select_from_digits_phone_number() returns void as
    $$
    begin
        set role to storage;
        perform from digits.phone_number limit 0;
    end;
    $$ language plpgsql;

end;
$mig$ language plpgsql;


create or replace function migrations.revert_digits_00002_table_digits_phone_number() returns void as
$$
begin
    drop table digits.phone_number;
end;
$$ language plpgsql;

select
from migrations.apply('digits',
                      2,
                      'table_digits_phone_number',
                      'Create the digits.phone_number table.');
