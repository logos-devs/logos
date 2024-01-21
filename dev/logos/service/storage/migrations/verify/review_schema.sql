-- Verify conjure:review_schema on pg

DO $$
BEGIN
   ASSERT (SELECT has_schema_privilege('review', 'usage'));
END $$;
