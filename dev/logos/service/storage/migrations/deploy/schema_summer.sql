-- Deploy logos:schema_summer to pg

BEGIN;

create schema summer;

grant usage on schema summer to storage;

COMMIT;
