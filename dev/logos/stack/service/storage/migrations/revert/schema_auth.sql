-- Revert logos:schema_auth from pg

BEGIN;

drop schema auth cascade;

COMMIT;
