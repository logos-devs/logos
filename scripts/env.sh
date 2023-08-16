#!/bin/sh -eu

PG_AUTH_HOST="db-rw.logos.dev"

CONSOLE_POD_NAME="pod/$(kubectl get pods -l app=console -o jsonpath="{.items[0].metadata.name}")"

kubectl wait --for=condition=Ready "$CONSOLE_POD_NAME"

STORAGE_PG_BACKEND_HOST="$(kubectl exec "$CONSOLE_POD_NAME" -- nslookup -type=cname db-rw.logos.dev | grep "canonical name = " | cut -d' ' -f4 | sed -e 's/\.$//')"

export STORAGE_PG_BACKEND_HOST
export STORAGE_PG_BACKEND_JDBC_URL="jdbc:postgresql://localhost:15432/logos"
export STORAGE_PG_BACKEND_USER="storage"
