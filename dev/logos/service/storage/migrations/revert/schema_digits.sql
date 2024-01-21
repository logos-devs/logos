-- Revert conjure:schema_digits from pg

BEGIN;

drop schema digits cascade;
-- XXX Add DDLs here.

COMMIT;
