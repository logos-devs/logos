-- Deploy logos:review_projects_table to pg
-- requires: review_schema

BEGIN;

create table review.project
(
    id           uuid primary key default uuid_generate_v4(),
    display_name varchar(255) not null,
    fetch_url    varchar(255) not null
);

COMMIT;
