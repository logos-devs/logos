-- Deploy logos:review_schema to pg

BEGIN;

create schema review;

grant usage on schema review to storage;

COMMIT;
