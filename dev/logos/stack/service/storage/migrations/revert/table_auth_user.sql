-- Revert logos:table_auth_credential from pg

begin;

drop table auth.user cascade;

commit;
