create or replace function migrations.apply_review_00002_table_review_project() returns void as
$mig$
begin

    create table review.project
    (
        id uuid primary key default uuid_generate_v4(),
        name varchar(255) not null
    );

    grant select, insert, update, delete on review.project to storage;

    create or replace function tests.role_storage_can_select_from_review_project() returns void as
    $$
    begin
        set role to storage;
        perform from review.project limit 0;
    end;
    $$ language plpgsql;

end;
$mig$ language plpgsql;


create or replace function migrations.revert_review_00002_table_review_project() returns void as
$$
begin
    drop table review.project;
end;
$$ language plpgsql;

select
from migrations.apply('review',
                      2,
                      'table_review_project',
                      'Create the review.project table.');
