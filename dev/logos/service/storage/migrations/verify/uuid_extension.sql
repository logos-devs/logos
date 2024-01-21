-- Verify conjure:uuid_extension on pg

BEGIN;

select uuid_generate_v4();

ROLLBACK;
