#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/deploy.yml"
SUDO="sudo"
ACTION="${1:-}"
shift || true

usage() {
  cat >&2 <<'EOF'
Usage: ./deploy/ingestion/service.sh <start|stop|restart|status> [kafka|doris-fe|doris-be|doris|all]
       [--config deploy.yml] [--sudo sudo]
EOF
}

if [[ "$ACTION" != "start" && "$ACTION" != "stop" && "$ACTION" != "restart" && "$ACTION" != "status" ]]; then usage; exit 2; fi
SERVICE="all"
while [[ $# -gt 0 ]]; do
  case "$1" in
    -c|--config) CONFIG_FILE="$2"; shift 2 ;;
    --sudo) SUDO="$2"; shift 2 ;;
    kafka|doris-fe|doris-be|doris|all) SERVICE="$1"; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 2 ;;
  esac
done

source "$SCRIPT_DIR/common.sh"
load_vars
mkdir -p "$SCRIPT_DIR/.runtime/ssh-control"
chmod 700 "$SCRIPT_DIR/.runtime/ssh-control" 2>/dev/null || true

docker_action() {
  local host="$1" user="$2" port="$3" name="$4" command
  case "$ACTION" in
    start) command="$SUDO docker start $name" ;;
    stop) command="$SUDO docker stop $name" ;;
    restart) command="$SUDO docker restart $name" ;;
    status) command="$SUDO docker ps -a --filter name=^/$name$" ;;
  esac
  remote_exec "$host" "$user" "$port" "$command"
}

handle_kafka() {
  local token="$1"; docker_action "$(node_host "$token")" "${VARS[kafka_ssh_user]}" "${VARS[kafka_ssh_port]}" "datamaster-ingestion-kafka-$(node_id "$token")"
}

handle_doris_fe() {
  local token="$1"; docker_action "$(node_host "$token")" "${VARS[doris_ssh_user]}" "${VARS[doris_ssh_port]}" "datamaster-ingestion-doris-fe-$(node_id "$token")"
}

handle_doris_be() {
  local token="$1"; docker_action "$(node_host "$token")" "${VARS[doris_ssh_user]}" "${VARS[doris_ssh_port]}" "datamaster-ingestion-doris-be-$(node_id "$token")"
}

run_group() { for_node_spec "$1" "$2"; }

case "$SERVICE" in
  kafka) run_group "${VARS[kafka_nodes]}" handle_kafka ;;
  doris-fe) run_group "${VARS[doris_fe_nodes]}" handle_doris_fe ;;
  doris-be) run_group "${VARS[doris_be_nodes]}" handle_doris_be ;;
  doris)
    if [[ "$ACTION" == "stop" ]]; then
      run_group "${VARS[doris_be_nodes]}" handle_doris_be
      run_group "${VARS[doris_fe_nodes]}" handle_doris_fe
    else
      run_group "${VARS[doris_fe_nodes]}" handle_doris_fe
      run_group "${VARS[doris_be_nodes]}" handle_doris_be
    fi
    ;;
  all)
    if [[ "$ACTION" == "stop" ]]; then
      run_group "${VARS[doris_be_nodes]}" handle_doris_be
      run_group "${VARS[doris_fe_nodes]}" handle_doris_fe
      run_group "${VARS[kafka_nodes]}" handle_kafka
    else
      run_group "${VARS[kafka_nodes]}" handle_kafka
      run_group "${VARS[doris_fe_nodes]}" handle_doris_fe
      run_group "${VARS[doris_be_nodes]}" handle_doris_be
    fi
    ;;
esac
