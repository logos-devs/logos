-- Verify logos:table_summer_topic_entry on pg

BEGIN;

set role to storage;

select * from summer.topic_entry limit 0;

ROLLBACK;
