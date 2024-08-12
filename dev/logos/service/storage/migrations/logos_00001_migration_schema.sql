begin;

create schema if not exists migrations;

create or replace function migrations.apply_logos_00001_migration_schema() returns void as
$mig$
begin
    create schema assert;
    create extension if not exists "uuid-ossp";

    create function assert.equal(
        expected anyelement,
        actual anyelement,
        message text
    ) returns void as
    $$
    begin
        if expected != actual then
            raise exception 'Expected % but got %: %', expected, actual, message;
        end if;
    end;
    $$ language plpgsql;

    create function assert.true(
        condition boolean,
        message text
    ) returns void as
    $$
    select assert.equal(true, condition, message);
    $$ language sql;


    create table migrations.project
    (
        id uuid primary key default gen_random_uuid(),
        name text not null unique check (name ~ '^[a-z][a-z0-9_]*$'),
        description text not null
    );

    create function migrations.project(_name varchar, _description varchar) returns uuid as
    $$
    insert into migrations.project (name, description)
    values (_name, _description)
    on conflict (name) do update set description = excluded.description
    returning id;
    $$ language sql;

    perform migrations.project('logos', 'Framework-level schemata.');


    create table migrations.migration
    (
        id uuid primary key default gen_random_uuid(),
        migration_number int not null,
        project_id uuid not null references migrations.project (id),
        name text not null unique check (name ~ '^[a-z][a-z0-9_]*$'),
        description text not null check (length(description) > 10),
        apply_function regproc not null,
        revert_function regproc not null
    );


    create schema tests;

    create function tests.schema_migrations_exists() returns void as
    $$
    select assert.true(
                   (select (select count(*)
                            from information_schema.schemata
                            where schema_name = 'migrations') = 1),
                   'The migrations schema should exist.');
    $$ language sql;


    create or replace function tests.run()
        returns table
                (
                    test_name varchar,
                    passed boolean
                )
    as
    $$
    declare
        func record;
    begin
        for func in select routine_name
                    from information_schema.routines
                    where specific_schema = 'tests'
                      and routine_type = 'FUNCTION'
                      and routine_name <> 'run'
                      and data_type = 'void'
            loop
                begin
                    execute format('select tests.%I()', func.routine_name);
                    test_name := func.routine_name;
                    passed := true;
                    raise notice 'tests.% passed', func.routine_name;
                    return next;

                    raise exception sqlstate 'R9999';

                exception
                    when sqlstate 'R9999' then

                    when others then
                        test_name = func.routine_name;
                        passed := false;
                        raise notice 'tests.% failed', func.routine_name;
                        return next;
                end;

                reset role;
            end loop;
        return;
    end;
    $$ language plpgsql;


    create function migrations.head(_project_name varchar) returns int as
    $$

    select max(migration_number)
    from migrations.migration
    where project_id = (select id
                        from migrations.project
                        where name = _project_name);
    $$ language sql;


    create or replace function migrations.apply(
        _project_name varchar,
        _migration_number int,
        _migration_name varchar,
        _description varchar
    ) returns integer as
    $$
    declare
        _existing_migration_number integer;
        _created_migration_number integer;
        _project_id uuid;
        apply_func_schema text;
        apply_func_name text;
    begin
        select id
        into _project_id
        from migrations.project
        where name = _project_name;

        select migration_number
        into _existing_migration_number
        from migrations.migration
        where project_id = _project_id
          and migration_number = _migration_number;

        if _existing_migration_number is not null then
            raise notice 'Migration % already exists.', _existing_migration_number;
            return _existing_migration_number;
        end if;

        insert into migrations.migration (migration_number, project_id, name, description, apply_function,
                                          revert_function)
        values (coalesce((select max(migration_number) + 1 from migrations.migration where project_id = _project_id),
                         1),
                _project_id,
                _migration_name,
                _description,
                ('migrations.apply_' || _project_name || '_' || lpad(_migration_number::text, 5, '0') || '_' ||
                 _migration_name)::regproc,
                ('migrations.revert_' || _project_name || '_' || lpad(_migration_number::text, 5, '0') || '_' ||
                 _migration_name)::regproc)
        returning migration_number
            into _created_migration_number;

        perform assert.true(_created_migration_number = _migration_number, 'Migration number should match the id.');

        select n.nspname, p.proname
        into apply_func_schema, apply_func_name
        from pg_proc p
                 join pg_namespace n on p.pronamespace = n.oid
        where p.oid = (select apply_function
                       from migrations.migration
                       where migration_number = _migration_number
                         and project_id = _project_id);

        execute format('select %I.%I();', apply_func_schema, apply_func_name);

        perform assert.true(_migration_number = migrations.head(_project_name), 'The migration should be the head.');
        perform assert.true((select bool_and(passed) from tests.run()), 'All tests should pass.');

        return _created_migration_number;
    end;
    $$ language plpgsql;


    create or replace function migrations.revert(
        _project_name varchar,
        _migration_number int
    ) returns void as
    $$
    declare
        _project_id uuid;
        func_schema text;
        func_name text;
    begin
        select id
        into _project_id
        from migration.project
        where name = _project_name;

        select n.nspname, p.proname
        into func_schema, func_name
        from pg_proc p
                 join pg_namespace n on p.pronamespace = n.oid
        where p.oid = (select revert_function
                       from migrations.migration
                       where migration_number = _migration_number
                         and project_id = _project_id);

        if func_schema is null then
            raise exception 'Migration % does not exist.', _migration_number;
        end if;

        delete
        from migrations.migration
        where project_id = _project_id
          and migration_number = _migration_number;

        execute format('select %I.%I();', func_schema, func_name);

        perform assert.true((select bool_and(passed) from tests.run()), 'All tests should pass.');
    end;
    $$ language plpgsql;

end ;
$mig$ language plpgsql;


create or replace function migrations.revert_logos_00001_migration_schema() returns void as
$$
begin
    drop schema migrations cascade;
    drop schema tests cascade;
    drop schema assert cascade;
end;
$$ language plpgsql;


-- migrations.apply does not exist yet, so we need to insert the first migration manually.
do
$$
    begin
        if not exists (select
                       from information_schema.tables
                       where table_schema = 'migrations'
                         and table_name = 'migration') then
            perform migrations.apply_logos_00001_migration_schema();

            insert into migrations.migration (migration_number, project_id, name, description, apply_function,
                                              revert_function)
            values (1,
                    (select id from migrations.project where name = 'logos'),
                    'create_schema_migrations',
                    'Create the migrations schema.',
                    'migrations.apply_logos_00001_migration_schema',
                    'migrations.revert_logos_00001_migration_schema');

            perform assert.true((select bool_and(passed) from tests.run()), 'all tests should pass.');
        end if;
    end;
$$ language plpgsql;

commit;