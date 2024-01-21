-- Revert logos:role_storage from pg

BEGIN;

drop role storage;

COMMIT;