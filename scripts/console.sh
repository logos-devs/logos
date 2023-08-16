#!/bin/sh -e

exec kubectl exec -ti "$(kubectl get pod -l app=console -o name)"  -- "${@:-"sh"}"
