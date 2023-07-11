-- Verify logos:review_projects_table on pg

BEGIN;

select from review.project limit 0;

ROLLBACK;
