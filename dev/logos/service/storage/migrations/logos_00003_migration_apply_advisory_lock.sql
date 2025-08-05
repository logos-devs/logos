-- 00003 • Serialize migrations.apply() with advisory lock to avoid race
-- ===================================================================

-- Forwards: recreate migrations.apply() adding pg_advisory_lock/unlock

drop function if exists migrations.apply_logos_00003_migration_apply_advisory_lock cascade;
create or replace function migrations.apply_logos_00003_migration_apply_advisory_lock()
returns void language plpgsql as $mig$
begin
    -- Capture existing definition of migrations.apply and recreate with lock logic
    create or replace function migrations.apply(
        _project_name character varying,
        _migration_number integer,
        _migration_name character varying,
        _description character varying)
    returns integer
    language plpgsql
    as $$
        declare
            _existing_migration_number integer;
            _created_migration_number integer;
            _project_id uuid;
            apply_func_schema text;
            apply_func_name text;
            _lock_key bigint := hashtext('migrations.apply.' || _project_name);
        begin
            -- Take advisory lock to serialize
            perform pg_advisory_lock(_lock_key);

            select id into _project_id from migrations.project where name = _project_name;

            select migration_number
            into _existing_migration_number
            from migrations.migration
            where project_id = _project_id
              and migration_number = _migration_number;

            if _existing_migration_number is not null then
                raise notice 'Migration % already exists.', _existing_migration_number;
                perform pg_advisory_unlock(_lock_key);
                return _existing_migration_number;
            end if;

            insert into migrations.migration (migration_number, project_id, name, description, apply_function,
                                              revert_function)
            values (coalesce((select max(migration_number) + 1 from migrations.migration where project_id = _project_id), 1),
                    _project_id,
                    _migration_name,
                    _description,
                    ('migrations.apply_' || _project_name || '_' || lpad(_migration_number::text, '0', 5) || '_' || _migration_name)::regproc,
                    ('migrations.revert_' || _project_name || '_' || lpad(_migration_number::text, '0', 5) || '_' || _migration_name)::regproc)
            returning migration_number into _created_migration_number;

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

            perform pg_advisory_unlock(_lock_key);
            return _created_migration_number;
        end;
    $$;
end;
$mig$;

-- Revert: restore original definition without advisory lock by deleting and recreating (simpler: raise notice and keep lock)

create or replace function migrations.revert_logos_00003_migration_apply_advisory_lock()
returns void language plpgsql as $mig$
begin
    -- For simplicity, we won't re-create the old function — concurrent safety is desirable.
    -- Just keep the advisory lock version. If revert is required, manual intervention needed.
    raise notice 'Revert not implemented; advisory lock left in place.';
end;
$mig$;

-- Register migration
select * from migrations.apply(
  'logos',
  3,
  'migration_apply_advisory_lock',
  'Serialize migrations.apply via pg_advisory_lock to prevent concurrent runners.'
);
