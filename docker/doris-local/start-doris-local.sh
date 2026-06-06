#!/bin/bash
set -euo pipefail

DORIS_HOME=/opt/apache-doris
export PATH="${DORIS_HOME}/fe/bin:${DORIS_HOME}/be/bin:${PATH}"

stop_doris() {
  stop_be.sh || true
  stop_fe.sh || true
}

wait_for_sql() {
  for _ in $(seq 1 180); do
    if mysql -uroot -P9030 -h127.0.0.1 -e "show frontends" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

wait_for_be() {
  for _ in $(seq 1 180); do
    if mysql -uroot -P9030 -h127.0.0.1 -e "show backends" 2>/dev/null | grep -q "true"; then
      return 0
    fi
    sleep 2
  done
  return 1
}

trap stop_doris SIGTERM SIGINT

mkdir -p "${DORIS_HOME}/fe/doris-meta" "${DORIS_HOME}/be/storage" "${DORIS_HOME}/fe/log" "${DORIS_HOME}/be/log"
grep -q '^priority_networks *= *127.0.0.1/24' "${DORIS_HOME}/be/conf/be.conf" \
  || echo 'priority_networks = 127.0.0.1/24' >>"${DORIS_HOME}/be/conf/be.conf"

start_fe.sh --daemon
wait_for_sql

mysql -uroot -P9030 -h127.0.0.1 -e "alter system add backend '127.0.0.1:9050'" >/dev/null 2>&1 || true

start_be.sh --daemon
wait_for_be

echo "Apache Doris local container is ready."
tail -F "${DORIS_HOME}/fe/log/fe.log" "${DORIS_HOME}/be/log/be.INFO"
