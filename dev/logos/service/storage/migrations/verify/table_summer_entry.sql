-- Verify logos:table_summer_entry on pg

BEGIN;

set role to storage;

select * from summer.entry limit 0;

ROLLBACK;
