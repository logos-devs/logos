-- Verify logos:table_summer_topic on pg

BEGIN;

set role to storage;

select * from summer.topic limit 0;

ROLLBACK;
