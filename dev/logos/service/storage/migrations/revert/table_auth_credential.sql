-- Revert logos:table_auth_credential from pg

BEGIN;

drop table auth.credential cascade;

COMMIT;
