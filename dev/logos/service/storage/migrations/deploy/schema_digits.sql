-- Deploy conjure:schema_digits to pg

BEGIN;

create schema digits;

grant usage on schema digits to storage;

COMMIT;
