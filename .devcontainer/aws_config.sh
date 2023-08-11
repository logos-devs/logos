#!/bin/bash -eu

mkdir -p ~/.aws/

cat <<EOF > ~/.aws/config
[profile default]
sso_session = logos-dev
sso_account_id = ${AWS_SSO_ACCOUNT_ID}
sso_role_name = ${AWS_SSO_ROLE_NAME}
region = us-east-2

[sso-session logos-dev]
sso_start_url = ${AWS_SSO_START_URL}
sso_region = ${AWS_SSO_REGION}
sso_registration_scopes = sso:account:access
EOF