-- Revert logos:schema_summer from pg

BEGIN;

drop schema summer cascade;

COMMIT;
