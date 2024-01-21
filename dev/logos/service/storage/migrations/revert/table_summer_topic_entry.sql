-- Revert logos:table_summer_topic_entry from pg

BEGIN;

drop table summer.topic_entry cascade;

COMMIT;
