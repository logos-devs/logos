#!/bin/bash -e

# Directory containing this script.
WORKSPACE_DIR="$(dirname "$(realpath "$0")")/.."

stderr() {
  echo "$@" 2>&1
}

die() {
  stderr "$@"
  exit 1
}

make_password() {
  pwgen -s 30 1
}

update_hosts_file() {
  hostname="$1"; shift 1
  ip="$1"; shift 1
  echo "$hostname $ip"

  echo -e "\nAdding $hostname to /etc/hosts which may require your password for sudo.\n"
  sudo $SED -i "/\<$hostname\>/d" /etc/hosts
  echo -e "$ip\t$hostname" | sudo /usr/bin/tee -a /etc/hosts
}

update_bazelrc_build_define() {
  var_name="$1"; shift 1
  var_value="$1"; shift 1

  bazelrc_local="$WORKSPACE_DIR/.bazelrc.local"
  echo -e "\nUpdating $bazelrc_local : build --define=$var_name=$var_value"
  $SED -i "/^build --define=$var_name=/d" "$bazelrc_local" || true
  echo "build --define=$var_name=$var_value" >> "$bazelrc_local"
}

update_local_gitops_bzl() {
  var_name="$1"; shift 1
  var_value="$1"; shift 1

  bzl="$WORKSPACE_DIR/gitops_local.bzl"
  echo -e "\nUpdating $bzl : $var_name=$var_value"
  $SED -i "/^$var_name=/d" "$bzl" || true
  echo "$var_name=\"$var_value\"" >> "$bzl"
}

update_pgpass() {
  username="$1"; shift 1
  password="$1"; shift 1

  pgpass="$HOME/.pgpass"
  touch "$pgpass"
  echo -e "\nUpdating $pgpass with credentials for minikube stolon instance."
  $SED -i "/^minikube:30002:logos:$username:/d" "$pgpass"
  echo "minikube:30002:logos:$username:$password" >> "$pgpass"
}

PLATFORM="$(uname -s)"

command -v bazelisk || die "Please install bazelisk."
command -v mkcert || die "Please install mkcert."

if [ "$PLATFORM" = "Darwin" ]
then
    command -v brew || die "Please install homebrew. https://brew.sh/"

    brew list bash || brew install bash
    brew list gnu-sed || brew install gnu-sed
    brew list hyperkit || brew install hyperkit

    DRIVER="hyperkit"
    SED="gsed"
    mk_subnet="${MK_SUBNET:-"192.168.64"}"
else
    DRIVER="kvm2"
    SED="sed"
    mk_subnet="${MK_SUBNET:-"192.168.39"}"
    virt-host-validate
fi

bazel="bazelisk"

# TODO set cluster properly
minikube="$bazel run //:minikube -- -p dev "
kubectl="$bazel run //:kubectl -- --context dev "

$minikube delete
$minikube start \
  --driver="$DRIVER" \
  --cpus=2 \
  --insecure-registry "${mk_subnet}.0/24" \
  --subnet="$mk_subnet.0" \
  --addons ingress ingress-dns dashboard #metrics-server
$minikube addons enable registry

# add all development hostnames here to map them to the ingress IP
# TODO : pull hostnames from the ingress yaml definition itself, and heck, trigger an update to /etc/hosts when that changes, why not?
minikube_ip="$($minikube ip)"
minikube_host_ip="${mk_subnet}.1"

update_hosts_file "dev.digits.rip" "$minikube_ip"
update_hosts_file "dev.logos.dev"  "$minikube_ip"
update_hosts_file "dev.rep.dev"    "$minikube_ip"
update_hosts_file "minikube"       "$minikube_ip"
update_hosts_file "minikube-host"  "$minikube_host_ip"

touch "$WORKSPACE_DIR/gitops_local.bzl"
update_local_gitops_bzl "LOCAL_REGISTRY" "$minikube_ip:5000"

touch "$WORKSPACE_DIR/.bazelrc.local"
update_bazelrc_build_define minikube_host "$minikube_host_ip"
update_bazelrc_build_define minikube_repo "$minikube_ip:5000"

# secrets
PG_STOLON_PASSWORD="$(make_password)}"
$kubectl create secret generic stolon \
  --from-literal="password=$PG_STOLON_PASSWORD"

STORAGE_PG_BACKEND_USER="root"
STORAGE_PG_BACKEND_PASSWORD="$(make_password)}"
$kubectl create secret generic storage-pg-root-credentials \
  --from-literal="user=$STORAGE_PG_BACKEND_USER" \
  --from-literal="password=$STORAGE_PG_BACKEND_PASSWORD"

cat <<EOF > "$WORKSPACE_DIR/env"
export STORAGE_PG_BACKEND_JDBC_URL="jdbc:postgresql://minikube:30002/logos"
export STORAGE_PG_BACKEND_USER="$STORAGE_PG_BACKEND_USER"
export STORAGE_PG_BACKEND_PASSWORD="$STORAGE_PG_BACKEND_PASSWORD"
EOF

SOURCE_ENV_CMD="source $WORKSPACE_DIR/env"
grep "^$SOURCE_ENV_CMD\$" "$HOME/.bash_profile" || (echo "$SOURCE_ENV_CMD" >> "$HOME/.bash_profile")

update_pgpass "$STORAGE_PG_BACKEND_USER" "$STORAGE_PG_BACKEND_PASSWORD"

# TODO : generate a new development key each time.
SERVER_AUTH_PRIVATE_KEY="$(cat <<EOF
-----BEGIN PRIVATE KEY-----
MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCsgwphLVGnq5gH
cvRhn0IOJaKl3DyHNVtb1AqIj4J7/PLIx5iPFbSlSp3pyYKs2LpqLbx3zOGE6UIk
Voqq0rC4nc9YGOt0F+cJ7K9GshU3jmq4vYUTifjQWTu4DG/Y7F6hfZvfWlB+UeAS
MtUd0NRXs5UKZQpA4VSYd1oQgG4mmMwrC+th3Cv9b6BEpWXIFuR3f8Zxn/RFGbyh
BlhUycJa6KsJEVu0P8F3vsccP8TXMvK3sqb7qk+NsCpeYSSJ/xLzMs0FccvtlHVT
odwHWNVpo44I9FtETOYJBjQcJYuVKQ+hbdNvW3rz1tc18okctr/bA19rIp8kOhw0
Z1OTEnARAgMBAAECggEBAJCP8zfB6WZPdkPvwfi4o3sFcMn6x8IqJVfLVdRgJmFO
dAlqsV5eOxY0dCZAlj/QEk796qydxPJDIgkfeG5zxmG+5M0XfN+5VAPOEod0njED
KE2Ni6H3AclerZuq2GN4mEhN4TwHC+L/K300mcC1ievAconWaAQ8j06A3blsVmIC
UrkrrhULVxCfEjFCU+vVPZtLJwniPMDzwO7UK1YS9cRgL1837zejtqv8F1qgiSFh
GlDkGtGrJQV5Vc8xMpg4idFnbuSM8Qg/igJ8fn334W84uBSTb1a2WJiTR0QpQ17Y
PB53m9QJp4ojUJhe3zKWv5HJ+sfvoLnIyvCg5/tU6W0CgYEA2xea7taS3cYwBOwM
zgkZ1g8zMPjBCmBKYMSa2V+jPPTnZWoCOf/rLoWhEbp77USYna6/KkcKv18o6sri
Uv6tbK6NoXX8et6JsXTgvave0gA5GNERa80kF4TBCGfdwzTsHXbG/9KR1JgP61/E
q+dP1a1t6CK1l56+RD/N9jCCvAsCgYEAyZKmPL4IfPS5jzv0NRAwhCXCNoh1DM/A
hag8JQFX5T6sFfrWEIw5UmtVHMq8XvfogDMckvvGdZ9wo6GAvXJpk4nzKww2XcFs
GBs6qjrpBxDbj5m7UrmQCqFuvLy/alOp1jCxshWmsiXvCxHORRhmIrRY6o4p0W2x
tRwFdvc5OdMCgYEAw7RRPBlFtX6sNClawsyUXIbVVmBNp2Qd7FSScauhV/j3nbpU
5NQ6kyLgnsJyop3MqcWHk09ERW/OT/UMt9Awv80oTFrlPif83RwnCKY28mUqm4Vd
R2tGWw/FkimdiRqD80m/NpM0mq9+QOUZ++gygw9ZBqvCg/5TvQk1hD0O+sMCgYBC
yGx9OXS0eZw04WcyYW/BiIUE7kbhfL9LSQbMN9q4IACBs75SczLWpKrpRB4O1NHa
D5UK+ZGyDKYUAIEXwx1JfW3sODqW62t1vSe9mJD1/1bPB97xNHuNmhiHPX2pq5hc
V3u4BcZZxYmiQD7303KbUucWpw0hztcOqv8AgD4ccwKBgQCJtnfwuMlbucpyj1pZ
zAlXi3okj4FrZHXQxo7C2WaOvPMPPyCLuqnToAgHhFLBKIySkfC9yXZeicS+K9A0
c63YeEAEqZrFZizZkLhuQAS4gv4ye5choaFp79KRB44U+jBnMS8aoFR/QubEA/Jo
G2q1o4tFID8Ef0gpp88nOAWiBQ==
-----END PRIVATE KEY-----
EOF
)"

cat <<OUTER_EOF >> "$WORKSPACE_DIR/env"
export SERVER_AUTH_PRIVATE_KEY="\$(cat <<\EOF
$SERVER_AUTH_PRIVATE_KEY
EOF
)"
OUTER_EOF

$kubectl create secret generic server-auth-key --from-file=private-key=<(echo "$SERVER_AUTH_PRIVATE_KEY")

# generate dev ingress tls cert
mkcert -cert-file dev-cert.pem -key-file dev-cert.key dev.digits.rip dev.logos.dev dev.rep.dev
$kubectl create secret tls ingress-tls --cert=<(cat dev-cert.pem) --key=<(cat dev-cert.key)
rm dev-cert.pem dev-cert.key

# TODO tighten up this firewall rule. shouldn't allow requests from a LAN that happens to use the docker subnet
if command -v ufw
then
    sudo ufw allow from "${mk_subnet}.0/24" to any port 8080 # //client:webpack_dev_server
    sudo ufw allow from "${mk_subnet}.0/24" to any port 8081 # //server:service_hot
fi

# Aim the docker command line utility at the minikube docker repository
# shellcheck disable=SC2046
eval $($minikube docker-env)

# build and deploy stolon first, since we'll need postgresql running to export schema
$bazel run //dev/logos/stack/service/storage:storage.apply

# stolon cluster
$kubectl run -i -t stolonctl \
  --image=sorintlab/stolon:master-pg14 \
  --restart=Never \
  --rm \
  -- \
  /usr/local/bin/stolonctl \
    --cluster-name=kube-stolon \
    --store-backend=kubernetes \
    --kube-resource-kind=configmap \
    init

export PGUSER="stolon"
PGPASSWORD="$($kubectl get secret stolon -o jsonpath='{.data.password}' | base64 --decode)"
export PGPASSWORD

_psql() {
  psql --host "$minikube_ip" --port 30002 template1 "$@"
}

while ! _psql -c select
do
  stderr "Waiting for database..."
  sleep 5
done

_psql -c "create role root superuser password '$STORAGE_PG_BACKEND_PASSWORD' login"

export PGUSER="root"
export PGPASSWORD="$STORAGE_PG_BACKEND_PASSWORD"
_psql -c "create database logos owner root encoding utf8"

(cd dev/logos/stack/service/storage/migrations \
 && sqitch deploy -t "db:pg://root@${minikube_ip}:30002/logos")

# k8s objects
$bazel run //dev/logos/stack/service/client:client.apply
$bazel run //dev/logos/stack/service/debug:debug.apply
