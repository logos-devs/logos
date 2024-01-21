-- Verify logos:schema_auth on pg

BEGIN;

DO
$$
    BEGIN
        ASSERT (SELECT has_schema_privilege('auth', 'usage'));
    END
$$;

ROLLBACK;
