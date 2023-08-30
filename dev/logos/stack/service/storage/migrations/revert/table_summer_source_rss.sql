-- Revert logos:table_summer_source_rss from pg

BEGIN;

drop table summer.source_rss cascade;

COMMIT;
