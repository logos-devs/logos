load("//bzl:pg_migrate.bzl", "pg_migrate")

pg_migrate(
    name = "migrations",
    migrations = glob(["*.sql"]),
    head = "00002_storage_role.sql",
    visibility = [
        "//dev/logos/service/database:__pkg__"
    ]
)