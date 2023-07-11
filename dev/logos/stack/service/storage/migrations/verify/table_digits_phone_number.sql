-- Verify conjure:table_digits_phone_number on pg

BEGIN;

select * from digits.phone_number limit 0;
-- XXX Add verifications here.

ROLLBACK;
