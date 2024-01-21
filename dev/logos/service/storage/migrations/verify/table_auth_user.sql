-- Verify logos:table_auth_credential on pg

begin;

select
from auth.user
limit 0;

rollback;
