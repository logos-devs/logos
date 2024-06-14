load("//bzl:pg_migrate.bzl", "pg_migrate")

pg_migrate(
    name = "migrations",
    head = "00002_table_review_project.sql",
    migrations = glob(["*.sql"]),
    visibility = ["//app/review/storage:__pkg__"],
)
