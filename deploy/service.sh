#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/deploy.yml"
SUDO="sudo"

usage() {
  cat >&2 <<'EOF'
Usage:
  ./deploy/start [--config deploy.yml] [--sudo sudo] <service|all> [...]
  ./deploy/stop  [--config deploy.yml] [--sudo sudo] <service|all> [...]

Services:
  postgresql redis zookeeper dolphinscheduler dbgpt backend app nginx quality all
EOF
}

ACTION="${1:-}"
if [[ "$ACTION" != "start" && "$ACTION" != "stop" && "$ACTION" != "restart" && "$ACTION" != "status" ]]; then
  usage
  exit 2
fi
shift

while [[ $# -gt 0 ]]; do
  case "$1" in
    -c|--config)
      CONFIG_FILE="$2"
      shift 2
      ;;
    --sudo)
      SUDO="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      break
      ;;
  esac
done

if [[ $# -eq 0 ]]; then
  usage
  exit 2
fi

declare -A VARS=()
declare -a VAR_KEYS=()

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
}

strip_quotes() {
  local value="$1"
  if [[ "$value" =~ ^\"(.*)\"$ ]]; then
    printf '%s' "${BASH_REMATCH[1]}"
  elif [[ "$value" =~ ^\'(.*)\'$ ]]; then
    printf '%s' "${BASH_REMATCH[1]}"
  else
    printf '%s' "$value"
  fi
}

load_vars() {
  local line key value i k ref
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="$(trim "$line")"
    [[ -z "$line" || "$line" == \#* ]] && continue
    if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)[[:space:]]*:[[:space:]]*(.*)$ ]]; then
      key="${BASH_REMATCH[1]}"
      value="$(strip_quotes "$(trim "${BASH_REMATCH[2]}")")"
      if [[ -z "${VARS[$key]+x}" ]]; then
        VAR_KEYS+=("$key")
      fi
      VARS["$key"]="$value"
    fi
  done < "$CONFIG_FILE"

  for ((i = 0; i < 8; i++)); do
    for k in "${VAR_KEYS[@]}"; do
      for ref in "${VAR_KEYS[@]}"; do
        VARS["$k"]="${VARS[$k]//"{{ $ref }}"/${VARS[$ref]}}"
      done
    done
  done
}

target_of() {
  local host="$1" user="$2"
  if [[ -n "$user" ]]; then
    printf '%s@%s' "$user" "$host"
  else
    printf '%s' "$host"
  fi
}

sq() {
  printf "'%s'" "${1//\'/\'\\\'\'}"
}

remote_exec() {
  local prefix="$1" command="$2" hosts user port host target
  hosts="${VARS[${prefix}_ssh_host]:-}"
  user="${VARS[${prefix}_ssh_user]:-}"
  port="${VARS[${prefix}_ssh_port]:-22}"
  if [[ -z "$hosts" ]]; then
    echo "Skip $prefix: ${prefix}_ssh_host is empty"
    return 0
  fi
  IFS=',' read -r -a host_items <<< "$hosts"
  for host in "${host_items[@]}"; do
    host="$(trim "$host")"
    [[ -z "$host" ]] && continue
    target="$(target_of "$host" "$user")"
    echo "ssh -p $port $target $command"
    ssh -p "$port" "$target" "$command"
  done
}

docker_cmd() {
  local container="$1"
  case "$ACTION" in
    start) printf '%s docker start %s' "$SUDO" "$container" ;;
    stop) printf '%s docker stop %s' "$SUDO" "$container" ;;
    restart) printf '%s docker restart %s' "$SUDO" "$container" ;;
    status) printf '%s docker ps -a --filter name=^/%s$' "$SUDO" "$container" ;;
  esac
}

systemd_cmd() {
  local unit="$1"
  case "$ACTION" in
    start) printf '%s systemctl start %s' "$SUDO" "$unit" ;;
    stop) printf '%s systemctl stop %s' "$SUDO" "$unit" ;;
    restart) printf '%s systemctl restart %s' "$SUDO" "$unit" ;;
    status) printf '%s systemctl --no-pager status %s' "$SUDO" "$unit" ;;
  esac
}

systemd_multi_cmd() {
  local units="$1"
  case "$ACTION" in
    start) printf '%s systemctl start %s' "$SUDO" "$units" ;;
    stop) printf '%s systemctl stop %s' "$SUDO" "$units" ;;
    restart) printf '%s systemctl restart %s' "$SUDO" "$units" ;;
    status) printf '%s systemctl --no-pager status %s' "$SUDO" "$units" ;;
  esac
}

handle_service() {
  local service="$1"
  case "$service" in
    postgresql|postgres|pg)
      remote_exec postgresql "$(docker_cmd datamaster-postgresql)"
      ;;
    redis)
      remote_exec redis "$(docker_cmd datamaster-redis)"
      ;;
    zookeeper|zk)
      remote_exec zookeeper "$(systemd_cmd datamaster-zookeeper)"
      ;;
    dolphinscheduler|ds)
      remote_exec dolphinscheduler "$(systemd_multi_cmd 'datamaster-dolphinscheduler-api datamaster-dolphinscheduler-master datamaster-dolphinscheduler-worker datamaster-dolphinscheduler-alert')"
      ;;
    dbgpt)
      remote_exec dbgpt "$(docker_cmd datamaster-dbgpt)"
      ;;
    backend|app|datamaster-server)
      remote_exec datamaster_app "$(systemd_cmd datamaster-server)"
      ;;
    nginx)
      remote_exec nginx "$(docker_cmd datamaster-nginx)"
      ;;
    quality)
      remote_exec datamaster_quality "$(docker_cmd datamaster-quality)"
      ;;
    all)
      if [[ "$ACTION" == "stop" ]]; then
        handle_service nginx
        handle_service backend
        handle_service dbgpt
        handle_service dolphinscheduler
        handle_service zookeeper
        handle_service redis
        handle_service postgresql
      else
        handle_service postgresql
        handle_service redis
        handle_service zookeeper
        handle_service dolphinscheduler
        handle_service dbgpt
        handle_service backend
        handle_service nginx
      fi
      ;;
    *)
      echo "Unknown service: $service" >&2
      usage
      exit 2
      ;;
  esac
}

load_vars
for service in "$@"; do
  handle_service "$service"
done
