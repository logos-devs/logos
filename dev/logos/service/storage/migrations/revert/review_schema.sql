-- Revert logos:review_schema from pg

BEGIN;

drop schema review;

COMMIT;
