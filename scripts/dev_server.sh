#!/bin/bash -eu

WORKSPACE_DIR="$(dirname "$(realpath "$0")")/.."

trap 'cleanup' ERR

cleanup() {
    if [[ -n ${TUNNEL_PID:-} ]] && kill -0 "$TUNNEL_PID"
    then
        echo "Killing PID: $TUNNEL_PID"
        kill "$TUNNEL_PID"
    fi
}

CONSOLE_POD_NAME="$(kubectl get pods -l app=console -o jsonpath="{.items[0].metadata.name}")"

STORAGE_PG_BACKEND_HOST=$(kubectl exec "$CONSOLE_POD_NAME" -- dig +short cname db-rw.logos.dev | sed -e 's/.$//')
export STORAGE_PG_BACKEND_HOST

export STORAGE_PG_BACKEND_JDBC_URL="jdbc:postgresql://:15432/logos"
export STORAGE_PG_BACKEND_USER="storage"

"$WORKSPACE_DIR"/scripts/db_tunnel.sh &
TUNNEL_PID=$!
echo "Started database tunnel at PID: $TUNNEL_PID"

sleep 3

ibazel run //dev/logos/stack/service/backend:Server -- --debug