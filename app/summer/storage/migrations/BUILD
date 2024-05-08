load("//bzl:pg_migrate.bzl", "pg_migrate")

pg_migrate(
    name = "migrations",
    head = "00005_table_summer_source_imap.sql",
    migrations = glob(["*.sql"]),
    visibility = ["//app/summer/storage:__pkg__"],
)
