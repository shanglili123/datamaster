#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/deploy.yml"
SUDO="sudo"
ACTION="list"
HOST_OVERRIDE=""
SSH_CONTROL_DIR="$SCRIPT_DIR/.runtime/ssh-control"
SSH_OPTS=(
  -o ControlMaster=auto
  -o ControlPersist=10m
  -o ControlPath="$SSH_CONTROL_DIR/%C"
)

usage() {
  cat >&2 <<'EOF'
Usage:
  ./deploy/rollback.sh [--config deploy.yml] [--sudo sudo] [--host host] --list <component> <remote-path>
  ./deploy/rollback.sh [--config deploy.yml] [--sudo sudo] [--host host] --restore <component> <remote-path>

Components:
  postgresql, redis, dolphinscheduler, dbgpt, datamaster_app, nginx

The path must be below base_dir. --restore is destructive for that one path
and never touches PostgreSQL or Redis data directories automatically.
EOF
}

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
    --host)
      HOST_OVERRIDE="$2"
      shift 2
      ;;
    --list)
      ACTION="list"
      shift
      ;;
    --restore)
      ACTION="restore"
      shift
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

if [[ $# -ne 2 ]]; then
  usage
  exit 2
fi
if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Config file not found: $CONFIG_FILE" >&2
  exit 1
fi
if ! command -v ssh >/dev/null 2>&1; then
  echo "ssh was not found. Install OpenSSH client first." >&2
  exit 1
fi
mkdir -p "$SSH_CONTROL_DIR"
chmod 700 "$SSH_CONTROL_DIR" 2>/dev/null || true

COMPONENT="$1"
REMOTE_PATH="$2"

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
  local line key value ref pass
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

  for ((pass = 0; pass < 8; pass++)); do
    for key in "${VAR_KEYS[@]}"; do
      for ref in "${VAR_KEYS[@]}"; do
        VARS["$key"]="${VARS[$key]//"{{ $ref }}"/${VARS[$ref]}}"
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

component_prefix() {
  case "$1" in
    postgresql|postgres|pg) printf 'postgresql' ;;
    redis) printf 'redis' ;;
    dolphinscheduler|ds) printf 'dolphinscheduler' ;;
    dbgpt) printf 'dbgpt' ;;
    datamaster_app|backend|app) printf 'datamaster_app' ;;
    nginx) printf 'nginx' ;;
    *)
      echo "Unknown component: $1" >&2
      exit 2
      ;;
  esac
}

remote_exec() {
  local host="$1" user="$2" port="$3" command="$4" target
  target="$(target_of "$host" "$user")"
  echo "ssh -p $port $target $command"
  ssh "${SSH_OPTS[@]}" -p "$port" "$target" "$command"
}

remote_capture() {
  local host="$1" user="$2" port="$3" command="$4" target
  target="$(target_of "$host" "$user")"
  ssh "${SSH_OPTS[@]}" -p "$port" "$target" "$command"
}

load_vars

PREFIX="$(component_prefix "$COMPONENT")"
HOSTS="${VARS[${PREFIX}_ssh_host]:-}"
USER_NAME="${VARS[${PREFIX}_ssh_user]:-}"
PORT="${VARS[${PREFIX}_ssh_port]:-22}"
BACKUP_DIR="${VARS[deployment_backup_dir]:-}"
BASE_DIR="${VARS[base_dir]:-}"

if [[ -z "$HOSTS" || -z "$BACKUP_DIR" || -z "$BASE_DIR" ]]; then
  echo "Config must define ${PREFIX}_ssh_host, base_dir and deployment_backup_dir" >&2
  exit 1
fi
case "$REMOTE_PATH" in
  "$BASE_DIR"/*) ;;
  *)
    echo "Refusing path outside base_dir: $REMOTE_PATH" >&2
    exit 1
    ;;
esac
if [[ "$REMOTE_PATH" == "$BASE_DIR" ]]; then
  echo "Refusing to rollback base_dir itself" >&2
  exit 1
fi
for protected_path in "${VARS[postgresql_data_dir]:-}" "${VARS[redis_data_dir]:-}" "$BACKUP_DIR"; do
  [[ -z "$protected_path" ]] && continue
  if [[ "$REMOTE_PATH" == "$protected_path" || "$REMOTE_PATH" == "$protected_path"/* ]]; then
    echo "Refusing to rollback protected path: $REMOTE_PATH" >&2
    exit 1
  fi
done

IFS=',' read -r -a HOST_ITEMS <<< "$HOSTS"
HOST=""
HOST_COUNT=0
for configured_host in "${HOST_ITEMS[@]}"; do
  configured_host="$(trim "$configured_host")"
  [[ -z "$configured_host" ]] && continue
  HOST_COUNT=$((HOST_COUNT + 1))
  if [[ -n "$HOST_OVERRIDE" && "$configured_host" == "$HOST_OVERRIDE" ]]; then
    HOST="$configured_host"
  elif [[ -z "$HOST_OVERRIDE" && -z "$HOST" ]]; then
    HOST="$configured_host"
  fi
done
if [[ -n "$HOST_OVERRIDE" && -z "$HOST" ]]; then
  echo "Host $HOST_OVERRIDE is not configured for $PREFIX" >&2
  exit 1
fi
if [[ -z "$HOST_OVERRIDE" && "$HOST_COUNT" -gt 1 ]]; then
  echo "Component $PREFIX has multiple hosts; specify one with --host. Configured: $HOSTS" >&2
  exit 1
fi
[[ -n "$HOST" ]] || { echo "No host configured for $PREFIX" >&2; exit 1; }

MANIFEST="$BACKUP_DIR/manifest.tsv"
TARGET_QUOTED="$(sq "$REMOTE_PATH")"
MANIFEST_QUOTED="$(sq "$MANIFEST")"

if [[ "$ACTION" == "list" ]]; then
  LIST_COMMAND="if ! $SUDO test -f $MANIFEST_QUOTED; then echo 'No deployment backup manifest found'; exit 1; fi; $SUDO awk -F '\\t' -v target=$TARGET_QUOTED '\$2 == target { print \"timestamp=\" \$1 \" backup=\" \$3 }' $MANIFEST_QUOTED | tail -n 10"
  remote_exec "$HOST" "$USER_NAME" "$PORT" "$LIST_COMMAND"
  exit 0
fi

LOOKUP_COMMAND="$SUDO awk -F '\\t' -v target=$TARGET_QUOTED '\$2 == target { path=\$3 } END { print path }' $MANIFEST_QUOTED | tr -d '\\r\\n'"
BACKUP_PATH="$(remote_capture "$HOST" "$USER_NAME" "$PORT" "$LOOKUP_COMMAND")"
case "$BACKUP_PATH" in
  "$BACKUP_DIR"/*) ;;
  *)
    echo "No safe backup found for $REMOTE_PATH" >&2
    exit 1
    ;;
esac

BACKUP_QUOTED="$(sq "$BACKUP_PATH")"
RESTORE_COMMAND="if ! $SUDO test -e $BACKUP_QUOTED; then echo 'Backup target no longer exists'; exit 1; fi; if $SUDO test -d $BACKUP_QUOTED; then $SUDO rm -rf $TARGET_QUOTED && $SUDO cp -al $BACKUP_QUOTED $TARGET_QUOTED; else $SUDO install -D -m \$(stat -c '%a' $BACKUP_QUOTED) $BACKUP_QUOTED $TARGET_QUOTED; fi; echo 'Rollback completed for $REMOTE_PATH from $BACKUP_PATH'"
remote_exec "$HOST" "$USER_NAME" "$PORT" "$RESTORE_COMMAND"
