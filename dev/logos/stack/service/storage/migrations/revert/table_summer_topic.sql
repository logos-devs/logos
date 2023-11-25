-- Revert logos:table_summer_topic from pg

BEGIN;

drop table summer.topic cascade;

COMMIT;
