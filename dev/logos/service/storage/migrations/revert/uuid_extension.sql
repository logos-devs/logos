-- Revert logos:uuid_extension from pg

BEGIN;

drop extension "uuid-ossp";

COMMIT;
