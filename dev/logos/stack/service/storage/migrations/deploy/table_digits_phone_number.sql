-- Deploy conjure:table_digits_phone_number to pg

BEGIN;

create table digits.phone_number (
  id uuid primary key default uuid_generate_v4(),
  phone_number varchar not null
);

COMMIT;
