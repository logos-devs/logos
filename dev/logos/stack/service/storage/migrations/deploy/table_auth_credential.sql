-- Deploy logos:table_auth_credential to pg

begin;

create table auth.credential
(
    id                 uuid primary key default uuid_generate_v4(),
    username           varchar not null unique,
    display_name       varchar not null,
    user_handle        bytea,
    key_id             bytea,
    public_key_cose    bytea,
    discoverable       bool,
    signature_count    integer,
    attestation_object bytea,
    client_data_json   varchar
);

grant select, update, delete on auth.credential to storage;

commit;
