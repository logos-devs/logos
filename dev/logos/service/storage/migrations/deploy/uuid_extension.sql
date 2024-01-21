-- Deploy logos:uuid_extension to pg

BEGIN;

create extension "uuid-ossp";

COMMIT;
