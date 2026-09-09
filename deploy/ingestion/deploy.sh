#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/deploy.yml"
SUDO="sudo"
LIMIT="kafka_servers,doris_fe_servers,doris_be_servers"
CHECK_ONLY=0

usage() {
  cat >&2 <<'EOF'
Usage: ./deploy/ingestion/deploy.sh [--config deploy.yml] [--limit group[,group]] [--sudo sudo] [--check]

Groups:
  kafka_servers, doris_fe_servers, doris_be_servers, doris_servers

This script deploys only the ingestion infrastructure. It does not deploy the
DataMaster main service.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -c|--config) CONFIG_FILE="$2"; shift 2 ;;
    -l|--limit) LIMIT="$2"; shift 2 ;;
    --sudo) SUDO="$2"; shift 2 ;;
    --check) CHECK_ONLY=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 2 ;;
  esac
done

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Ingestion config not found: $CONFIG_FILE" >&2
  exit 1
fi
if ! command -v ssh >/dev/null 2>&1 || ! command -v scp >/dev/null 2>&1; then
  echo "ssh and scp are required" >&2
  exit 1
fi
mkdir -p "$SCRIPT_DIR/.runtime/ssh-control"
chmod 700 "$SCRIPT_DIR/.runtime/ssh-control" 2>/dev/null || true

source "$SCRIPT_DIR/common.sh"

should_deploy_group() {
  local group="$1" item
  IFS=',' read -r -a items <<< "$LIMIT"
  for item in "${items[@]}"; do
    item="$(trim "$item")"
    [[ "$item" == "$group" ]] && return 0
    [[ "$item" == "doris_servers" && ( "$group" == "doris_fe_servers" || "$group" == "doris_be_servers" ) ]] && return 0
  done
  return 1
}

validate_nodes() {
  local key="$1" minimum="$2" count
  require_var "$key"
  count="$(node_count "${VARS[$key]}")"
  if [[ "$count" -lt "$minimum" ]]; then
    echo "$key must contain at least $minimum nodes for a cluster; got $count" >&2
    exit 1
  fi
}

validate_config() {
  local key
  for key in timezone remote_package_dir kafka_ssh_user kafka_ssh_port kafka_cluster_id kafka_image kafka_broker_port kafka_controller_port kafka_topic_partitions kafka_topic_replication_factor kafka_data_dir doris_ssh_user doris_ssh_port doris_fe_image doris_be_image doris_fe_http_port doris_fe_query_port doris_fe_editlog_port doris_be_http_port doris_be_heartbeat_port doris_be_brpc_port doris_fe_data_dir doris_be_data_dir; do
    require_var "$key"
  done
  if should_deploy_group kafka_servers; then validate_nodes kafka_nodes 3; fi
  if should_deploy_group doris_fe_servers; then validate_nodes doris_fe_nodes 3; fi
  if should_deploy_group doris_be_servers; then validate_nodes doris_be_nodes 3; fi
}

deploy_kafka_node() {
  local token="$1" id host quorum image_tar
  id="$(node_id "$token")"
  host="$(node_host "$token")"
  quorum="$(kafka_quorum_voters)"
  image_tar="$(resolve_local_path "${VARS[kafka_image_tar_src]}")"
  echo "Deploying Kafka broker $id to ${VARS[kafka_ssh_user]}@$host..."
  remote_exec "$host" "${VARS[kafka_ssh_user]}" "${VARS[kafka_ssh_port]}" "$SUDO mkdir -p $(sq "${VARS[kafka_data_dir]}/$id") $(sq "${VARS[remote_package_dir]}") && $SUDO chmod 0777 $(sq "${VARS[kafka_data_dir]}/$id")"
  load_docker_image_if_present "$host" "${VARS[kafka_ssh_user]}" "${VARS[kafka_ssh_port]}" "$image_tar" "${VARS[kafka_image_tar]}"
  remote_exec "$host" "${VARS[kafka_ssh_user]}" "${VARS[kafka_ssh_port]}" "$SUDO docker rm -f datamaster-ingestion-kafka-$id >/dev/null 2>&1 || true; $SUDO docker run -d --name datamaster-ingestion-kafka-$id --hostname datamaster-ingestion-kafka-$id --restart always --network host -e KAFKA_NODE_ID=$(sq "$id") -e KAFKA_PROCESS_ROLES=broker,controller -e KAFKA_LISTENERS=$(sq "PLAINTEXT://:${VARS[kafka_broker_port]},CONTROLLER://:${VARS[kafka_controller_port]}") -e KAFKA_ADVERTISED_LISTENERS=$(sq "PLAINTEXT://${host}:${VARS[kafka_broker_port]}") -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER -e KAFKA_CONTROLLER_QUORUM_VOTERS=$(sq "$quorum") -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT -e KAFKA_LOG_DIRS=/var/lib/kafka/data -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=$(sq "${VARS[kafka_topic_replication_factor]}") -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=$(sq "${VARS[kafka_topic_replication_factor]}") -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=2 -e KAFKA_MIN_INSYNC_REPLICAS=2 -e KAFKA_DEFAULT_REPLICATION_FACTOR=$(sq "${VARS[kafka_topic_replication_factor]}") -e KAFKA_NUM_PARTITIONS=$(sq "${VARS[kafka_topic_partitions]}") -e CLUSTER_ID=$(sq "${VARS[kafka_cluster_id]}") -v $(sq "${VARS[kafka_data_dir]}/$id"):/var/lib/kafka/data:Z $(sq "${VARS[kafka_image]}")"
}

create_kafka_topics() {
  local token host id topic bootstrap
  token="$(trim "${VARS[kafka_nodes]%%,*}")"
  id="$(node_id "$token")"
  host="$(node_host "$token")"
  bootstrap="${host}:${VARS[kafka_broker_port]}"
  remote_exec "$host" "${VARS[kafka_ssh_user]}" "${VARS[kafka_ssh_port]}" "for i in \$(seq 1 60); do $SUDO docker exec datamaster-ingestion-kafka-$id /opt/kafka/bin/kafka-topics.sh --bootstrap-server $(sq "$bootstrap") --list >/dev/null 2>&1 && exit 0; sleep 2; done; exit 1"
  IFS=',' read -r -a topics <<< "${VARS[kafka_topics]:-}"
  for topic in "${topics[@]}"; do
    topic="$(trim "$topic")"
    topic="${topic#[}"; topic="${topic%]}"; topic="${topic//\"/}"; topic="${topic//\'/}"
    [[ -z "$topic" ]] && continue
    remote_exec "$host" "${VARS[kafka_ssh_user]}" "${VARS[kafka_ssh_port]}" "$SUDO docker exec datamaster-ingestion-kafka-$id /opt/kafka/bin/kafka-topics.sh --bootstrap-server $(sq "$bootstrap") --create --if-not-exists --topic $(sq "$topic") --partitions ${VARS[kafka_topic_partitions]} --replication-factor ${VARS[kafka_topic_replication_factor]}"
  done
}

deploy_doris_fe_node() {
  local token="$1" id host fe_servers image_tar
  id="$(node_id "$token")"; host="$(node_host "$token")"; fe_servers="$(doris_fe_servers)"
  image_tar="$(resolve_local_path "${VARS[doris_fe_image_tar_src]}")"
  echo "Deploying Doris FE $id to ${VARS[doris_ssh_user]}@$host..."
  remote_exec "$host" "${VARS[doris_ssh_user]}" "${VARS[doris_ssh_port]}" "$SUDO sysctl -w vm.max_map_count=2000000 >/dev/null 2>&1 || true; $SUDO mkdir -p $(sq "${VARS[doris_fe_data_dir]}/$id/doris-meta") $(sq "${VARS[doris_fe_data_dir]}/$id/log") $(sq "${VARS[remote_package_dir]}") && $SUDO chmod -R 0777 $(sq "${VARS[doris_fe_data_dir]}/$id")"
  load_docker_image_if_present "$host" "${VARS[doris_ssh_user]}" "${VARS[doris_ssh_port]}" "$image_tar" "${VARS[doris_fe_image_tar]}"
  remote_exec "$host" "${VARS[doris_ssh_user]}" "${VARS[doris_ssh_port]}" "$SUDO docker rm -f datamaster-ingestion-doris-fe-$id >/dev/null 2>&1 || true; $SUDO docker run -d --name datamaster-ingestion-doris-fe-$id --hostname datamaster-ingestion-doris-fe-$id --restart always --network host --privileged -e FE_SERVERS=$(sq "$fe_servers") -e FE_ID=$(sq "$id") -v $(sq "${VARS[doris_fe_data_dir]}/$id/doris-meta"):/opt/apache-doris/fe/doris-meta:Z -v $(sq "${VARS[doris_fe_data_dir]}/$id/log"):/opt/apache-doris/fe/log:Z $(sq "${VARS[doris_fe_image]}")"
}

deploy_doris_be_node() {
  local token="$1" id host fe_servers image_tar
  id="$(node_id "$token")"; host="$(node_host "$token")"; fe_servers="$(doris_fe_servers)"
  image_tar="$(resolve_local_path "${VARS[doris_be_image_tar_src]}")"
  echo "Deploying Doris BE $id to ${VARS[doris_ssh_user]}@$host..."
  remote_exec "$host" "${VARS[doris_ssh_user]}" "${VARS[doris_ssh_port]}" "$SUDO sysctl -w vm.max_map_count=2000000 >/dev/null 2>&1 || true; $SUDO mkdir -p $(sq "${VARS[doris_be_data_dir]}/$id/storage") $(sq "${VARS[doris_be_data_dir]}/$id/log") $(sq "${VARS[remote_package_dir]}") && $SUDO chmod -R 0777 $(sq "${VARS[doris_be_data_dir]}/$id")"
  load_docker_image_if_present "$host" "${VARS[doris_ssh_user]}" "${VARS[doris_ssh_port]}" "$image_tar" "${VARS[doris_be_image_tar]}"
  remote_exec "$host" "${VARS[doris_ssh_user]}" "${VARS[doris_ssh_port]}" "$SUDO docker rm -f datamaster-ingestion-doris-be-$id >/dev/null 2>&1 || true; $SUDO docker run -d --name datamaster-ingestion-doris-be-$id --hostname datamaster-ingestion-doris-be-$id --restart always --network host --privileged -e FE_SERVERS=$(sq "$fe_servers") -e BE_ADDR=$(sq "${host}:${VARS[doris_be_heartbeat_port]}") -v $(sq "${VARS[doris_be_data_dir]}/$id/storage"):/opt/apache-doris/be/storage:Z -v $(sq "${VARS[doris_be_data_dir]}/$id/log"):/opt/apache-doris/be/log:Z $(sq "${VARS[doris_be_image]}")"
}

write_endpoint_file() {
  local first_fe="${VARS[doris_fe_nodes]%%,*}"
  mkdir -p "$SCRIPT_DIR/.runtime"
  cat > "$SCRIPT_DIR/.runtime/endpoints.env" <<EOF
KAFKA_BOOTSTRAP_SERVERS=$(kafka_bootstrap_servers)
DORIS_FE_QUERY_ENDPOINT=$(node_host "$first_fe"):${VARS[doris_fe_query_port]}
DORIS_FE_HTTP_ENDPOINT=$(node_host "$first_fe"):${VARS[doris_fe_http_port]}
EOF
  chmod 600 "$SCRIPT_DIR/.runtime/endpoints.env"
}

load_vars
validate_config
if [[ "$CHECK_ONLY" -eq 1 ]]; then
  echo "Ingestion cluster config check passed: $CONFIG_FILE"
  exit 0
fi

if should_deploy_group kafka_servers; then
  for_node_spec "${VARS[kafka_nodes]}" deploy_kafka_node
  create_kafka_topics
fi
if should_deploy_group doris_fe_servers; then
  for_node_spec "${VARS[doris_fe_nodes]}" deploy_doris_fe_node
fi
if should_deploy_group doris_be_servers; then
  for_node_spec "${VARS[doris_be_nodes]}" deploy_doris_be_node
fi
write_endpoint_file
echo "Ingestion cluster deployment finished. Endpoints: $SCRIPT_DIR/.runtime/endpoints.env"
