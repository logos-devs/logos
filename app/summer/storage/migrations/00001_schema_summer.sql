select
from migrations.project('summer', 'summer.app');


create or replace function migrations.apply_summer_00001_schema_summer() returns void as
$mig$
begin

    create schema summer;
    grant usage on schema summer to storage;

    create or replace function tests.role_storage_can_use_schema_summer() returns void as
    $$
    select assert.true((select has_schema_privilege('storage'::regrole, 'public'::regnamespace, 'USAGE')),
                       'The storage role must be granted usage on schema summer.');
    $$ language sql;
end;
$mig$ language plpgsql;


create or replace function migrations.revert_summer_00001_schema_summer() returns void as
$$
begin
    drop function tests.role_storage_can_use_schema_summer();
    drop schema summer cascade;

    delete from migrations.project where name = 'summer';
end;
$$ language plpgsql;



select
from migrations.apply('summer',
                      1,
                      'schema_summer',
                      'Create the summer schema.');