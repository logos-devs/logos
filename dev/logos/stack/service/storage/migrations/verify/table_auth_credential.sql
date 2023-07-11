-- Verify logos:table_auth_credential on pg

BEGIN;

select
from auth.credential
limit 0;

ROLLBACK;
