load("//bzl:pg_migrate.bzl", "pg_migrate")

pg_migrate(
    name = "migrations",
    head = "logos_00003_migration_apply_advisory_lock.sql",
    migrations = glob(["*.sql"]),
    visibility = [
        "//dev/logos/service/backend:__pkg__",
    ],
)
