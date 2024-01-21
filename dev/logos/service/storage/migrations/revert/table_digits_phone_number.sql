-- Revert conjure:table_digits_phone_number from pg

BEGIN;

drop table digits.phone_number cascade;

COMMIT;
