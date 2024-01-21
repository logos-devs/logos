-- Revert logos:table_summer_entry from pg

BEGIN;

drop table summer.entry cascade;

COMMIT;
