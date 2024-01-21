-- Revert logos:review_projects_table from pg

BEGIN;

drop table review.project;

COMMIT;
