#!/bin/sh -e

PGPASSWORD="$(kubectl get secret storage-pg-root-credentials -o jsonpath='{.data.password}' | base64 --decode)"
export PGPASSWORD

exec psql --username root --host "$(bazel run //:minikube -- -p dev ip)" --port 30002 "${@:-"logos"}"
