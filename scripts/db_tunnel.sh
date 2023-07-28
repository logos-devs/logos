#!/bin/bash -eu

LOCAL_PORT=15432
REMOTE_PORT=5432

trap 'disconnect' INT TERM

CONSOLE_POD_NAME="$(kubectl get pods -l app=console -o jsonpath="{.items[0].metadata.name}")"
kubectl exec "$CONSOLE_POD_NAME" -- pkill -SIGQUIT socat || true

function disconnect() {
    printf "\nDisconnecting database tunnel.\n"
    kill "$PORT_FORWARD_PID"
    kill "$SOCAT_PID"
    kubectl exec "$CONSOLE_POD_NAME" -- pkill -SIGQUIT socat
}

kubectl exec "$CONSOLE_POD_NAME" -- socat "TCP-LISTEN:$REMOTE_PORT,fork,reuseaddr" "TCP:db-rw.logos.dev:$REMOTE_PORT" &
SOCAT_PID="$!"

kubectl port-forward "$CONSOLE_POD_NAME" "$LOCAL_PORT:$REMOTE_PORT" &
PORT_FORWARD_PID="$!"

while true; do sleep 1; done