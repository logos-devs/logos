-- Verify conjure:schema_digits on pg

BEGIN;

DO $$
    BEGIN
        ASSERT (SELECT has_schema_privilege('digits', 'usage'));
    END $$;

ROLLBACK;
