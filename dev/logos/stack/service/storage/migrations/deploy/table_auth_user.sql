-- Deploy logos:table_auth_credential to pg

begin;

create table auth.user
(
    id           uuid primary key default uuid_generate_v4(),
    username     varchar not null unique,
    display_name varchar not null
);

alter table auth.credential
    add column user_id uuid
        references auth.user (id);

commit;
