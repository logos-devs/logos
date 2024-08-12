load("//bzl:pg_migrate.bzl", "pg_migrate")

pg_migrate(
    name = "migrations",
    head = "logos_00002_storage_role.sql",
    migrations = glob(["*.sql"]),
    visibility = [
        "//dev/logos/service/backend:__pkg__",
    ],
)
