-- Verify logos:table_summer_source_rss on pg

BEGIN;

set role to storage;

select * from summer.source_rss limit 0;

ROLLBACK;
