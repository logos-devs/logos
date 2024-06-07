select
from migrations.project('review', 'review.logos.dev');


create or replace function migrations.apply_review_00001_schema_review() returns void as
$mig$
begin

    create schema review;
    grant usage on schema review to storage;

    create or replace function tests.role_storage_can_use_schema_review() returns void as
    $$
    select assert.true((select has_schema_privilege('storage'::regrole, 'public'::regnamespace, 'USAGE')),
                       'The storage role must be granted usage on schema review.');
    $$ language sql;
end;
$mig$ language plpgsql;


create or replace function migrations.revert_review_00001_schema_review() returns void as
$$
begin
    drop function tests.role_storage_can_use_schema_review();
    drop schema review cascade;

    delete from migrations.project where name = 'review';
end;
$$ language plpgsql;



select
from migrations.apply('review',
                      1,
                      'schema_review',
                      'Create the review schema.');
