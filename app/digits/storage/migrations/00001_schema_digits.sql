select
from migrations.project('digits', 'digits.rip');


create or replace function migrations.apply_digits_00001_schema_digits() returns void as
$mig$
begin

    create schema digits;
    grant usage on schema digits to storage;

    create or replace function tests.role_storage_can_use_schema_digits() returns void as
    $$
    select assert.true((select has_schema_privilege('storage'::regrole, 'public'::regnamespace, 'USAGE')),
                       'The storage role must be granted usage on schema digits.');
    $$ language sql;
end;
$mig$ language plpgsql;


create or replace function migrations.revert_digits_00001_schema_digits() returns void as
$$
begin
    drop function tests.role_storage_can_use_schema_digits();
    drop schema digits cascade;

    delete from migrations.project where name = 'digits';
end;
$$ language plpgsql;



select
from migrations.apply('digits',
                      1,
                      'schema_digits',
                      'Create the digits schema.');
