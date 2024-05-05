load("//bzl:pg_migrate.bzl", "pg_migrate")

pg_migrate(
    name = "migrations",
    migrations = glob(["*.sql"]),
    head = "00004_table_summer_topic.sql",
    visibility = ["//app/summer/storage:__pkg__"],
)