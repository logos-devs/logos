create or replace function migrations.apply_logos_00002_role_storage() returns void as
$mig$
begin
    create or replace function create_role_if_not_exists(role_name text) returns void as $$
    begin
        execute format('create role %I', role_name);
    exception
        when duplicate_object then raise notice '%, skipping', SQLERRM using errcode = SQLSTATE;
    end;
    $$ language plpgsql;

    perform create_role_if_not_exists('storage');

    alter role storage with login;
    grant rds_iam to storage;

    create or replace function tests.role_storage_can_login() returns void as
    $$
    select assert.true(rolcanlogin, 'The storage role should be able to login.')
    from pg_roles
    where rolname = 'storage';
    $$ language sql;

    create or replace function tests.role_storage_has_rds_iam() returns void as
    $$
    select assert.true(exists (select 1
                               from pg_roles r
                                        join
                                    pg_auth_members m on r.oid = m.roleid
                               where r.rolname = 'rds_iam'
                                 and m.member = (select oid
                                                 from pg_roles
                                                 where rolname = 'storage')),
                       'The storage role should be granted the rds_iam role.');
    $$ language sql;
end ;
$mig$ language plpgsql;


create or replace function migrations.revert_logos_00002_role_storage() returns void as
$rev$
begin
    drop function if exists tests.role_storage_can_login();
    drop function if exists tests.role_storage_has_rds_iam();
    --drop role storage;
end;
$rev$ language plpgsql;


select
from migrations.apply(
        'logos',
        2,
        'role_storage',
        'Create the storage role.'
     );