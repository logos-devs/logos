-- Deploy logos:schema_auth to pg

BEGIN;

create schema auth;

grant usage on schema auth to storage;

COMMIT;
