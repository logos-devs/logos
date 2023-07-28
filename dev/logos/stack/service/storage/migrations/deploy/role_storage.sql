-- Deploy logos:role_storage to pg

BEGIN;

create role storage login;
grant rds_iam to storage;

COMMIT;
