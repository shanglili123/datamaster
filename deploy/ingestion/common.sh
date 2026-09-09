#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${CONFIG_FILE:-$SCRIPT_DIR/deploy.yml}"
SUDO="${SUDO:-sudo}"
SSH_CONTROL_DIR="$SCRIPT_DIR/.runtime/ssh-control"
SSH_OPTS=(-o ControlMaster=auto -o ControlPersist=10m -o ControlPath="$SSH_CONTROL_DIR/%C")

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
      if [[ -z "${VARS[$key]+x}" ]]; then VAR_KEYS+=("$key"); fi
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

sq() { printf "'%s'" "${1//\'/\'\\\'\'}"; }

remote_exec() {
  local host="$1" user="$2" port="$3" command="$4"
  echo "ssh -p $port ${user}@${host} $command"
  ssh "${SSH_OPTS[@]}" -p "$port" "${user}@${host}" "$command"
}

resolve_local_path() {
  local path="$1"
  if [[ "$path" = /* ]]; then printf '%s' "$path"; else (cd "$SCRIPT_DIR/.." && printf '%s/%s' "$(pwd)" "$path"); fi
}

require_var() {
  local key="$1"
  if [[ -z "${VARS[$key]+x}" || -z "${VARS[$key]}" || "${VARS[$key]}" == "null" ]]; then
    echo "Missing required ingestion deploy.yml value: $key" >&2
    exit 1
  fi
}

load_docker_image_if_present() {
  local host="$1" user="$2" port="$3" local_tar="$4" remote_name="$5" remote_path
  if [[ -z "$local_tar" || ! -f "$local_tar" ]]; then
    echo "Skip docker load: $local_tar not found; use an image already present on $host"
    return 0
  fi
  remote_path="${VARS[remote_package_dir]}/$remote_name"
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "${VARS[remote_package_dir]}")"
  echo "scp $local_tar ${user}@${host}:$remote_path"
  scp "${SSH_OPTS[@]}" -P "$port" "$local_tar" "${user}@${host}:$remote_path"
  remote_exec "$host" "$user" "$port" "$SUDO docker load -i $(sq "$remote_path")"
}

node_id() { printf '%s' "${1%%@*}"; }
node_host() { printf '%s' "${1#*@}"; }

for_node_spec() {
  local list="$1" callback="$2" token
  IFS=',' read -r -a tokens <<< "$list"
  for token in "${tokens[@]}"; do
    token="$(trim "$token")"
    [[ -z "$token" ]] && continue
    "$callback" "$token"
  done
}

kafka_quorum_voters() {
  local result="" token id host
  IFS=',' read -r -a tokens <<< "${VARS[kafka_nodes]}"
  for token in "${tokens[@]}"; do
    token="$(trim "$token")"; [[ -z "$token" ]] && continue
    id="$(node_id "$token")"; host="$(node_host "$token")"
    result="${result:+$result,}${id}@${host}:${VARS[kafka_controller_port]}"
  done
  printf '%s' "$result"
}

kafka_bootstrap_servers() {
  local result="" token host
  IFS=',' read -r -a tokens <<< "${VARS[kafka_nodes]}"
  for token in "${tokens[@]}"; do
    token="$(trim "$token")"; [[ -z "$token" ]] && continue
    host="$(node_host "$token")"
    result="${result:+$result,}${host}:${VARS[kafka_broker_port]}"
  done
  printf '%s' "$result"
}

doris_fe_servers() {
  local result="" token id host
  IFS=',' read -r -a tokens <<< "${VARS[doris_fe_nodes]}"
  for token in "${tokens[@]}"; do
    token="$(trim "$token")"; [[ -z "$token" ]] && continue
    id="$(node_id "$token")"; host="$(node_host "$token")"
    result="${result:+$result,}fe${id}:${host}:${VARS[doris_fe_editlog_port]}"
  done
  printf '%s' "$result"
}

node_count() {
  local list="$1" count=0 token
  IFS=',' read -r -a tokens <<< "$list"
  for token in "${tokens[@]}"; do [[ -n "$(trim "$token")" ]] && count=$((count + 1)); done
  printf '%s' "$count"
}
