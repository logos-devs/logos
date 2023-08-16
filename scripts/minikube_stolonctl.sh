#!/bin/sh -e

exec kubectl run \
  -i -t stolonctl \
  --image=bazel/stack/service/storage:image \
  --restart=Never \
  --rm \
  -- /usr/local/bin/stolonctl \
     --cluster-name=kube-stolon \
     --store-backend=kubernetes \
     --kube-resource-kind=configmap \
     "$@"
