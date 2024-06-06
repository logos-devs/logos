load("//bzl:pg_migrate.bzl", "pg_migrate")

pg_migrate(
    name = "migrations",
    head = "00002_table_digits_phone_number.sql",
    migrations = glob(["*.sql"]),
    visibility = ["//app/digits/storage:__pkg__"],
)
