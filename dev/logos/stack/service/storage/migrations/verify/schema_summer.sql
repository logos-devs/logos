-- Verify logos:schema_summer on pg

BEGIN;

set role to storage;

DO $$
    BEGIN
        ASSERT (SELECT has_schema_privilege('summer', 'usage'));
    END $$;

ROLLBACK;
