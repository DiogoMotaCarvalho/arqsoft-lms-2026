#!/bin/sh
set -eu

if [ "$#" -ne 0 ] && [ "$#" -ne 3 ]; then
  echo 'Usage: setup-environments.sh [DEV_PORT PP_PORT PROD_PORT]' >&2
  exit 1
fi
dev_port=${1:-8181}
pp_port=${2:-8182}
prod_port=${3:-8183}
for port in "$dev_port" "$pp_port" "$prod_port"; do
  case "$port" in
    '' | *[!0-9]*) echo "Invalid port: $port" >&2; exit 1 ;;
  esac
  if [ "${#port}" -gt 5 ] || [ "$port" -lt 1024 ] || [ "$port" -gt 65535 ]; then
    echo "Use private ports between 1024 and 65535: $port" >&2
    exit 1
  fi
done
if [ "$dev_port" -eq "$pp_port" ] || [ "$dev_port" -eq "$prod_port" ] || [ "$pp_port" -eq "$prod_port" ]; then
  echo 'The three ports must be different.' >&2
  exit 1
fi

test "$(id -u)" -eq 0
id deploy >/dev/null
test -x /usr/bin/java

install -m 644 psoft-g1@.service /etc/systemd/system/psoft-g1@.service
install -d -m 755 /etc/psoft-g1
for name in dev pp prod; do
  case "$name" in
    dev) port=$dev_port ;;
    pp) port=$pp_port ;;
    prod) port=$prod_port ;;
  esac
  if ! id "psoft-g1-$name" >/dev/null 2>&1; then
    useradd --system --no-create-home --shell /usr/sbin/nologin "psoft-g1-$name"
  fi
  install -d -o deploy -g "psoft-g1-$name" -m 755 "/opt/psoft-g1/$name"
  if [ ! -e "/etc/psoft-g1/$name.env" ]; then
    cat > "/etc/psoft-g1/$name.env" <<EOF
APP_ENVIRONMENT=$name
SERVER_PORT=$port
SPRING_DATASOURCE_URL=jdbc:h2:mem:psoft;IGNORECASE=TRUE
FILE_UPLOADDIR=/var/lib/psoft-g1/$name/uploads
EOF
    chmod 644 "/etc/psoft-g1/$name.env"
  else
    echo "Kept existing /etc/psoft-g1/$name.env"
  fi
done

visudo -cf psoft-g1.sudoers
install -m 440 psoft-g1.sudoers /etc/sudoers.d/psoft-g1
systemctl daemon-reload
systemctl enable psoft-g1@dev psoft-g1@pp psoft-g1@prod
