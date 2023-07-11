#!/bin/sh -e

exec kubectl exec -ti "$(kubectl get pod -l app=debug -o name)"  -- "${@:-"sh"}"
