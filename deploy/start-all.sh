#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATE_DIR="$SCRIPT_DIR/templates"
CONFIG_TEMPLATE_DIR="$SCRIPT_DIR/config"
CONFIG_FILE="$SCRIPT_DIR/deploy.yml"
LIMIT=""
DEFAULT_LIMIT="postgresql_servers,redis_servers,dolphinscheduler_servers,dbgpt_servers,datamaster_app_servers,nginx_servers"
SUDO="sudo"
CHECK_ONLY=0
REMOTE_CHECK_ONLY=0
HEALTH_CHECK_ONLY=0
POST_HEALTH_CHECK=0
SSH_CONTROL_DIR="$SCRIPT_DIR/.runtime/ssh-control"
SSH_OPTS=(
  -o ControlMaster=auto
  -o ControlPersist=10m
  -o ControlPath="$SSH_CONTROL_DIR/%C"
)

usage() {
  cat >&2 <<'EOF'
Usage: ./deploy/start-all.sh [--config deploy.yml] [--limit group[,group]] [--sudo sudo] [--check] [--remote-check] [--health-check] [--post-health-check]

Groups:
  postgresql_servers, redis_servers, dolphinscheduler_servers,
  dbgpt_servers, datamaster_app_servers, nginx_servers
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -c|--config)
      CONFIG_FILE="$2"
      shift 2
      ;;
    -l|--limit)
      LIMIT="$2"
      shift 2
      ;;
    --sudo)
      SUDO="$2"
      shift 2
      ;;
    --check)
      CHECK_ONLY=1
      shift
      ;;
    --remote-check)
      CHECK_ONLY=1
      REMOTE_CHECK_ONLY=1
      shift
      ;;
    --health-check)
      CHECK_ONLY=1
      HEALTH_CHECK_ONLY=1
      shift
      ;;
    --post-health-check)
      POST_HEALTH_CHECK=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 2
      ;;
  esac
done

if [[ -z "$LIMIT" ]]; then
  LIMIT="$DEFAULT_LIMIT"
fi

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Config file not found: $CONFIG_FILE" >&2
  exit 1
fi
if ! command -v ssh >/dev/null 2>&1; then
  echo "ssh was not found. Install OpenSSH client first." >&2
  exit 1
fi
if ! command -v scp >/dev/null 2>&1; then
  echo "scp was not found. Install OpenSSH client first." >&2
  exit 1
fi
mkdir -p "$SSH_CONTROL_DIR"
chmod 700 "$SSH_CONTROL_DIR" 2>/dev/null || true

declare -A VARS=()
declare -a VAR_KEYS=()
declare -a HOST_ROWS=()

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
  local line key value
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

  local k ref
  for k in "${VAR_KEYS[@]}"; do
    [[ "${VARS[$k]}" == *'{{ '*'}}'* ]] || continue
    for ref in "${VAR_KEYS[@]}"; do
      VARS["$k"]="${VARS[$k]//"{{ $ref }}"/${VARS[$ref]}}"
    done
  done
}

add_host_from_config() {
  local group="$1" prefix="$2" hosts user port host
  hosts="${VARS[${prefix}_ssh_host]:-}"
  user="${VARS[${prefix}_ssh_user]:-}"
  port="${VARS[${prefix}_ssh_port]:-22}"
  if [[ -z "$hosts" ]]; then
    if should_deploy_group "$group"; then
      echo "Missing ${prefix}_ssh_host in $CONFIG_FILE" >&2
      exit 1
    fi
    return 0
  fi
  IFS=',' read -r -a host_items <<< "$hosts"
  for host in "${host_items[@]}"; do
    host="$(trim "$host")"
    [[ -z "$host" ]] && continue
    HOST_ROWS+=("$group|$prefix|$host|$user|$port")
  done
}

load_hosts() {
  add_host_from_config postgresql_servers postgresql
  add_host_from_config redis_servers redis
  add_host_from_config dolphinscheduler_servers dolphinscheduler
  add_host_from_config dbgpt_servers dbgpt
  add_host_from_config datamaster_app_servers datamaster_app
  add_host_from_config nginx_servers nginx
}

add_var_key_if_missing() {
  local key="$1" existing
  for existing in "${VAR_KEYS[@]}"; do
    [[ "$existing" == "$key" ]] && return 0
  done
  VAR_KEYS+=("$key")
}

set_derived_var() {
  local key="$1" value="$2"
  VARS["$key"]="$value"
  add_var_key_if_missing "$key"
}

first_config_host() {
  local prefix="$1" hosts host
  hosts="${VARS[${prefix}_ssh_host]:-}"
  IFS=',' read -r host _ <<< "$hosts"
  trim "$host"
}

config_hosts_with_port() {
  local prefix="$1" port="$2" hosts host result=""
  hosts="${VARS[${prefix}_ssh_host]:-}"
  IFS=',' read -r -a host_items <<< "$hosts"
  for host in "${host_items[@]}"; do
    host="$(trim "$host")"
    [[ -z "$host" ]] && continue
    if [[ -n "$result" ]]; then
      result="$result,$host:$port"
    else
      result="$host:$port"
    fi
  done
  printf '%s' "$result"
}

derive_service_vars() {
  set_derived_var postgresql_ip "$(first_config_host postgresql)"
  set_derived_var redis_ip "$(first_config_host redis)"
  set_derived_var datamaster_app_ip "$(first_config_host datamaster_app)"
  set_derived_var dbgpt_ip "$(first_config_host dbgpt)"
}

var_has_placeholder() {
  local value="$1"
  [[ "$value" == \<*\> ]]
}

is_true() {
  local value="${1:-}"
  value="${value,,}"
  [[ "$value" == "1" || "$value" == "true" || "$value" == "yes" || "$value" == "on" ]]
}

deployment_backup_enabled() {
  is_true "${VARS[deployment_backup_enabled]:-false}"
}

backup_remote_target() {
  local host="$1" user="$2" port="$3" remote_path="$4"
  local backup_root stamp label backup_path manifest retention
  deployment_backup_enabled || return 0
  backup_root="${VARS[deployment_backup_dir]:-}"
  if [[ -z "$backup_root" ]]; then
    echo "deployment_backup_enabled=true but deployment_backup_dir is empty" >&2
    return 1
  fi
  stamp="$(date -u +%Y%m%dT%H%M%SZ)"
  label="$(printf '%s' "$remote_path" | tr '/: ' '___' | tr -cd '[:alnum:]_.-')"
  [[ -n "$label" ]] || label="target"
  backup_path="$backup_root/${stamp}_${label}"
  manifest="$backup_root/manifest.tsv"
  retention="${VARS[deployment_backup_retention]:-5}"
  remote_exec "$host" "$user" "$port" "set -e; $SUDO mkdir -p $(sq "$backup_root"); if $SUDO test -e $(sq "$remote_path") || $SUDO test -L $(sq "$remote_path"); then if $SUDO test -d $(sq "$remote_path"); then if ! $SUDO cp -al $(sq "$remote_path") $(sq "$backup_path"); then $SUDO rm -rf $(sq "$backup_path"); $SUDO cp -a $(sq "$remote_path") $(sq "$backup_path"); fi; else $SUDO cp -a $(sq "$remote_path") $(sq "$backup_path"); fi; printf '%s\\t%s\\t%s\\n' $(sq "$stamp") $(sq "$remote_path") $(sq "$backup_path") | $SUDO tee -a $(sq "$manifest") >/dev/null; echo "Backup created: $backup_path"; $SUDO find $(sq "$backup_root") -mindepth 1 -maxdepth 1 \( -type d -o -type f \) -name $(sq "*_${label}") -printf '%T@ %p\\n' | sort -nr | tail -n +$((retention + 1)) | cut -d' ' -f2- | while IFS= read -r old; do [ -z \"\$old\" ] || $SUDO rm -rf \"\$old\"; done; else echo "No existing target to backup: $remote_path"; fi"
}

validate_backup_config() {
  local backup_dir base_dir
  deployment_backup_enabled || return 0
  require_var deployment_backup_dir
  backup_dir="${VARS[deployment_backup_dir]}"
  base_dir="${VARS[base_dir]:-}"
  case "$backup_dir" in
    "$base_dir"/*) ;;
    *)
      echo "deployment_backup_dir must be below base_dir: $backup_dir" >&2
      return 1
      ;;
  esac
  if [[ -n "${VARS[deployment_backup_retention]:-}" && ! "${VARS[deployment_backup_retention]}" =~ ^[1-9][0-9]*$ ]]; then
    echo "deployment_backup_retention must be a positive integer" >&2
    return 1
  fi
}

security_defaults_allowed() {
  local environment="${VARS[deployment_environment]:-prod}"
  environment="${environment,,}"
  [[ "$environment" == "dev" || "$environment" == "development" || "$environment" == "test" || "$environment" == "local" ]] \
    || is_true "${VARS[allow_insecure_defaults]:-false}"
}

is_weak_secret() {
  local key="$1" value="${VARS[$1]:-}" username="${VARS[${2:-}]:-}"
  local normalized
  normalized="${value,,}"
  [[ -z "$value" ]] && return 0
  [[ "$normalized" == "postgres" || "$normalized" == "postgresql" || "$normalized" == "datamaster" \
    || "$normalized" == "password" || "$normalized" == "123456" || "$normalized" == "12345678" \
    || "$normalized" == "admin" || "$normalized" == "root" || "$normalized" == "changeme" \
    || "$normalized" == "changeit" || "$normalized" == "secret" || "$normalized" == "test" \
    || "$normalized" == "qwerty" ]] && return 0
  [[ -n "$username" && "$value" == "$username" ]] && return 0
  return 1
}

require_secure_secret() {
  local key="$1" username_key="${2:-}" label="${3:-$1}"
  local value="${VARS[$key]:-}"
  if [[ -z "$value" || "$value" == "null" ]]; then
    if security_defaults_allowed; then
      echo "WARNING: insecure or empty secret allowed for $label because deployment_environment=${VARS[deployment_environment]:-prod} and allow_insecure_defaults=${VARS[allow_insecure_defaults]:-false}" >&2
      return 0
    fi
    echo "Unsafe production configuration: $label must not be empty; set a secret or explicitly use deployment_environment=dev/test/local or allow_insecure_defaults=true." >&2
    return 1
  fi
  if is_weak_secret "$key" "$username_key"; then
    if security_defaults_allowed; then
      echo "WARNING: weak secret allowed for $label in non-production/insecure mode" >&2
      return 0
    fi
    echo "Unsafe production configuration: $label uses a default or weak secret; replace it before deployment." >&2
    return 1
  fi
  return 0
}

validate_security_config() {
  local failures=0
  local environment="${VARS[deployment_environment]:-prod}"
  if [[ -z "${VARS[deployment_environment]+x}" ]]; then
    echo "INFO: deployment_environment is not set; defaulting to prod" >&2
  fi
  case "${environment,,}" in
    prod|production|staging|pre|preprod|uat|dev|development|test|local) ;;
    *)
      echo "Invalid deployment_environment: $environment (expected prod, staging, uat, dev, test, or local)" >&2
      failures=1
      ;;
  esac

  if should_deploy_group postgresql_servers; then
    require_secure_secret postgresql_admin_password postgresql_admin_user "postgresql_admin_password" || failures=1
  fi
  if should_deploy_group postgresql_servers || should_deploy_group dolphinscheduler_servers || should_deploy_group datamaster_app_servers; then
    require_secure_secret postgresql_password postgresql_user "postgresql_password" || failures=1
  fi
  if should_deploy_group redis_servers || should_deploy_group datamaster_app_servers; then
    require_secure_secret redis_password "" "redis_password" || failures=1
  fi
  if should_deploy_group dbgpt_servers; then
    require_secure_secret dashscope_api_key "" "dashscope_api_key" || failures=1
  fi
  if should_deploy_group datamaster_app_servers; then
    if is_true "${VARS[ai_skill_model_enabled]:-false}"; then
      require_secure_secret dashscope_api_key "" "dashscope_api_key (AI skill model)" || failures=1
    fi
    require_secure_secret jwt_secret "" "jwt_secret" || failures=1
    if is_true "${VARS[ingestion_decision_trigger_enabled]:-false}" && [[ -n "${VARS[ingestion_decision_base_url]:-}" ]]; then
      require_secure_secret ingestion_decision_trigger_secret "" "ingestion_decision_trigger_secret" || failures=1
      if [[ -z "${VARS[ingestion_decision_trigger_ref]:-}" ]]; then
        echo "Unsafe configuration: ingestion_decision_trigger_ref is required when an external decision trigger is enabled." >&2
        failures=1
      fi
    fi
    if is_true "${VARS[lineage_enabled]:-false}"; then
      require_secure_secret neo4j_password neo4j_username "neo4j_password" || failures=1
    fi
  fi
  if [[ "$failures" -ne 0 ]]; then
    echo "Deployment security validation failed. No remote changes were made." >&2
    return 1
  fi
}

require_var() {
  local key="$1"
  if [[ -z "${VARS[$key]+x}" || -z "${VARS[$key]}" || "${VARS[$key]}" == "null" ]]; then
    echo "Missing required deploy.yml value: $key" >&2
    exit 1
  fi
  if var_has_placeholder "${VARS[$key]}"; then
    echo "Unresolved placeholder in deploy.yml: $key=${VARS[$key]}" >&2
    exit 1
  fi
}

require_defined() {
  local key="$1"
  if [[ -z "${VARS[$key]+x}" ]]; then
    echo "Missing deploy.yml key: $key" >&2
    exit 1
  fi
  if var_has_placeholder "${VARS[$key]}"; then
    echo "Unresolved placeholder in deploy.yml: $key=${VARS[$key]}" >&2
    exit 1
  fi
}

require_local_file_if_group() {
  local group="$1" key="$2" path entry
  local -a paths
  should_deploy_group "$group" || return 0
  require_var "$key"
  IFS=',' read -r -a paths <<< "${VARS[$key]}"
  for entry in "${paths[@]}"; do
    entry="${entry#${entry%%[![:space:]]*}}"
    entry="${entry%${entry##*[![:space:]]}}"
    path="$(resolve_local_path "$entry")"
    if [[ ! -f "$path" ]]; then
      echo "Required local file not found for $group: $path" >&2
      exit 1
    fi
  done
}

validate_config() {
  local key row row_group row_name row_host row_user row_port
  for key in base_dir timezone; do require_var "$key"; done
  if should_deploy_group postgresql_servers; then
    for key in postgresql_ip postgresql_port postgresql_image postgresql_image_tar postgresql_image_tar_src postgresql_admin_database postgresql_admin_user postgresql_admin_password postgresql_database postgresql_user postgresql_password database_init_jar_src postgresql_init_sql_src dolphinscheduler_database dolphinscheduler_init_sql_src dolphinscheduler_tenant_code dolphinscheduler_token datamaster_app_ip datamaster_server_port remote_package_dir remote_init_sql_dir postgresql_data_dir postgresql_conf_dir postgresql_log_dir; do require_var "$key"; done
  fi
  if should_deploy_group redis_servers; then
    for key in redis_port redis_image redis_image_tar redis_image_tar_src redis_data_dir redis_conf_dir redis_log_dir; do require_var "$key"; done
  fi
  if should_deploy_group dolphinscheduler_servers; then
    for key in dolphinscheduler_ip dolphinscheduler_package_src dolphinscheduler_install_tgz dolphinscheduler_dir dolphinscheduler_conf_dir dolphinscheduler_log_dir dolphinscheduler_resource_dir remote_soft_dir remote_package_dir soft_package_src chunjun_home flink_home hadoop_home spark_home datax_home postgresql_ip postgresql_port postgresql_database postgresql_user postgresql_password dolphinscheduler_database; do require_var "$key"; done
  fi
  if should_deploy_group dbgpt_servers; then
    for key in dbgpt_port dbgpt_image dbgpt_image_tar dbgpt_image_tar_src dbgpt_data_dir dbgpt_message_dir dashscope_api_key; do require_var "$key"; done
  fi
  if should_deploy_group datamaster_app_servers; then
    for key in datamaster_server_jar_src app_conf_dir app_log_dir app_upload_dir app_jar_path postgresql_ip postgresql_port postgresql_database postgresql_user postgresql_password redis_ip redis_port dolphinscheduler_ip dolphinscheduler_api_port dolphinscheduler_token dolphinscheduler_tenant_code dolphinscheduler_resource_dir datamaster_app_ip datamaster_server_port dbgpt_ip dbgpt_port dbgpt_model dbgpt_chat_mode dbgpt_skill_space_name dbgpt_skill_space_owner ai_skill_model_enabled ai_skill_model_api_url ai_skill_model_name ai_skill_model_timeout ai_skill_model_retry_count ai_skill_model_retry_interval ai_skill_model_temperature ai_skill_model_max_tokens ingestion_enabled ingestion_kafka_servers ingestion_kafka_topics ingestion_poll_timeout_ms ingestion_poll_interval_ms ingestion_kafka_max_retries ingestion_kafka_retry_backoff_ms ingestion_kafka_event_lease_ms ingestion_kafka_lease_renew_interval_ms ingestion_doris_datasource_id ingestion_conflict_strategy ingestion_key_columns ingestion_allowed_tables ingestion_required_columns ingestion_decision_trigger_enabled ingestion_decision_path lineage_enabled lineage_jdbc_enabled lineage_sync_delay_ms neo4j_uri neo4j_username; do require_var "$key"; done
    for key in redis_password dashscope_api_key jwt_secret neo4j_password ingestion_kafka_dead_letter_topic ingestion_target_table ingestion_schema_version ingestion_decision_base_url ingestion_decision_trigger_ref ingestion_decision_trigger_secret; do require_defined "$key"; done
  fi
  if should_deploy_group nginx_servers; then
    for key in nginx_port nginx_image nginx_image_tar nginx_image_tar_src nginx_conf_dir nginx_html_dir nginx_log_dir datamaster_ui_dist_src datamaster_app_ip datamaster_server_port; do require_var "$key"; done
  fi
  for row in "${HOST_ROWS[@]}"; do
    IFS='|' read -r row_group row_name row_host row_user row_port <<< "$row"
    should_deploy_group "$row_group" || continue
    require_var "${row_name}_ssh_host"
    require_var "${row_name}_ssh_user"
    require_var "${row_name}_ssh_port"
  done
  require_local_file_if_group postgresql_servers database_init_jar_src
  require_local_file_if_group postgresql_servers postgresql_init_sql_src
  require_local_file_if_group postgresql_servers dolphinscheduler_init_sql_src
  if [[ -n "${VARS[postgresql_app_upgrade_sql_src]:-}" ]]; then
    require_local_file_if_group postgresql_servers postgresql_app_upgrade_sql_src
  fi
  require_local_file_if_group datamaster_app_servers datamaster_server_jar_src
  if should_deploy_group nginx_servers; then
    local ui_dist
    ui_dist="$(resolve_local_path "${VARS[datamaster_ui_dist_src]}")"
    if [[ ! -d "$ui_dist" ]]; then
      echo "Required local directory not found for nginx_servers: $ui_dist" >&2
      exit 1
    fi
  fi
  validate_backup_config
  validate_security_config
}

should_deploy_group() {
  local group="$1" item
  [[ -z "$LIMIT" ]] && return 0
  IFS=',' read -r -a limit_items <<< "$LIMIT"
  for item in "${limit_items[@]}"; do
    [[ "$(trim "$item")" == "$group" ]] && return 0
  done
  return 1
}

for_each_host() {
  local group="$1" callback="$2" row found=0
  should_deploy_group "$group" || return 0
  for row in "${HOST_ROWS[@]}"; do
    IFS='|' read -r row_group row_name row_host row_user row_port <<< "$row"
    if [[ "$row_group" == "$group" ]]; then
      found=1
      "$callback" "$row_name" "$row_host" "$row_user" "$row_port"
    fi
  done
  if [[ "$found" -eq 0 ]]; then
    echo "No hosts configured for group '$group' in $CONFIG_FILE." >&2
    exit 1
  fi
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

redact_command() {
  local display="$1" key secret
  for key in postgresql_admin_password postgresql_password redis_password dashscope_api_key \
             ingestion_decision_trigger_secret neo4j_password dolphinscheduler_token; do
    secret="${VARS[$key]:-}"
    [[ -z "$secret" ]] && continue
    display="${display//"$secret"/<redacted>}"
  done
  printf '%s' "$display"
}

sql_literal() {
  local value="$1"
  value="${value//\'/\'\'}"
  printf "'%s'" "$value"
}

generate_token() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 16
  elif command -v uuidgen >/dev/null 2>&1; then
    uuidgen | tr -d '-' | tr '[:upper:]' '[:lower:]' | cut -c 1-32
  else
    date +%s%N | sha256sum | cut -c 1-32
  fi
}

ensure_runtime_vars() {
  local token_file token
  token_file="$SCRIPT_DIR/.runtime/dolphinscheduler.token"
  if (should_deploy_group postgresql_servers || should_deploy_group datamaster_app_servers) \
      && [[ -z "${VARS[dolphinscheduler_token]:-}" || "${VARS[dolphinscheduler_token]}" == "<DOLPHINSCHEDULER_TOKEN>" ]]; then
    if [[ "$CHECK_ONLY" -eq 1 ]]; then
      token="check-only-token"
      echo "DolphinScheduler token is empty; it will be generated during deployment."
    elif [[ -f "$token_file" ]]; then
      token="$(tr -d '\r\n' < "$token_file")"
    else
      mkdir -p "$(dirname "$token_file")"
      token="$(generate_token)"
      printf '%s\n' "$token" > "$token_file"
      chmod 600 "$token_file" 2>/dev/null || true
    fi
    VARS[dolphinscheduler_token]="$token"
    if [[ "$CHECK_ONLY" -ne 1 ]]; then
      echo "Using generated DolphinScheduler token from $token_file"
    fi
  fi
  if [[ -z "${VARS[dashscope_api_key]:-}" && -n "${DASHSCOPE_API_KEY:-}" ]]; then
    VARS[dashscope_api_key]="$DASHSCOPE_API_KEY"
  fi
}

remote_exec() {
  local host="$1" user="$2" port="$3" command="$4" target
  target="$(target_of "$host" "$user")"
  echo "ssh -p $port $target $(redact_command "$command")"
  ssh "${SSH_OPTS[@]}" -p "$port" "$target" "$command"
}

remote_exec_capture() {
  local host="$1" user="$2" port="$3" command="$4" target
  target="$(target_of "$host" "$user")"
  ssh "${SSH_OPTS[@]}" -p "$port" "$target" "$command"
}

remote_check_host() {
  local name="$1" host="$2" user="$3" port="$4"
  local command_list="" port_list="" path port_item
  local -a path_items=()
  local needs_docker=0 needs_java=0 needs_systemctl=0
  case "$name" in
    postgresql)
      needs_docker=1
      path_items=("${VARS[postgresql_data_dir]}" "${VARS[postgresql_conf_dir]}" "${VARS[postgresql_log_dir]}" "${VARS[remote_package_dir]}" "${VARS[remote_init_sql_dir]}")
      port_list="${VARS[postgresql_port]}"
      ;;
    redis)
      needs_docker=1
      path_items=("${VARS[redis_data_dir]}" "${VARS[redis_conf_dir]}" "${VARS[redis_log_dir]}")
      port_list="${VARS[redis_port]}"
      ;;
    dolphinscheduler)
      needs_java=1
      path_items=("${VARS[dolphinscheduler_dir]}" "${VARS[dolphinscheduler_conf_dir]}" "${VARS[dolphinscheduler_log_dir]}" "${VARS[dolphinscheduler_resource_dir]}" "${VARS[remote_soft_dir]}" "${VARS[remote_package_dir]}")
      port_list="${VARS[dolphinscheduler_api_port]:-12345}"
      ;;
    dbgpt)
      needs_docker=1
      path_items=("${VARS[dbgpt_data_dir]}" "${VARS[dbgpt_message_dir]}")
      port_list="${VARS[dbgpt_port]}"
      ;;
    datamaster_app)
      needs_java=1
      needs_systemctl=1
      path_items=("${VARS[app_conf_dir]}" "${VARS[app_log_dir]}" "${VARS[app_upload_dir]}")
      port_list="${VARS[datamaster_server_port]}"
      ;;
    nginx)
      needs_docker=1
      path_items=("${VARS[nginx_conf_dir]}" "${VARS[nginx_html_dir]}" "${VARS[nginx_log_dir]}")
      port_list="${VARS[nginx_port]}"
      ;;
    *)
      echo "Unknown remote-check host prefix: $name" >&2
      return 1
      ;;
  esac

  command_list="set -u; fail=0;"
  command_list+="if ! command -v ${SUDO%% *} >/dev/null 2>&1; then echo 'ERROR: sudo command not found'; fail=1; elif ! $SUDO -n true >/dev/null 2>&1; then echo 'ERROR: sudo requires an interactive password or is not permitted'; fail=1; fi;"
  command_list+="if ! command -v df >/dev/null 2>&1; then echo 'ERROR: df command not found'; fail=1; fi;"
  if [[ "$needs_docker" -eq 1 ]]; then
    command_list+="if ! command -v docker >/dev/null 2>&1; then echo 'ERROR: docker command not found'; fail=1; elif ! $SUDO -n docker info >/dev/null 2>&1; then echo 'ERROR: docker daemon is unavailable'; fail=1; fi;"
  fi
  if [[ "$needs_java" -eq 1 ]]; then
    command_list+="if ! command -v java >/dev/null 2>&1; then echo 'ERROR: java command not found'; fail=1; else java -version 2>&1 | head -n 1; fi;"
  fi
  if [[ "$needs_systemctl" -eq 1 ]]; then
    command_list+="if ! command -v systemctl >/dev/null 2>&1; then echo 'ERROR: systemctl command not found'; fail=1; fi;"
  fi
  command_list+="if ! command -v df >/dev/null 2>&1; then :; else free_mb=\$(df -Pm / | awk 'NR==2 {print \$4}'); if [ -n \"\$free_mb\" ] && [ \"\$free_mb\" -lt \"${VARS[remote_check_min_free_mb]:-1024}\" ]; then echo \"ERROR: root filesystem free space \${free_mb}MB is below ${VARS[remote_check_min_free_mb]:-1024}MB\"; fail=1; else echo \"disk free: \${free_mb:-unknown}MB\"; fi; fi;"
  for path in "${path_items[@]}"; do
    command_list+="target_path=$(sq "$path"); parent_path=\$(dirname \"\$target_path\"); if $SUDO test -d \"\$target_path\" && ! $SUDO test -w \"\$target_path\"; then echo \"ERROR: directory not writable: \$target_path\"; fail=1; elif ! $SUDO test -d \"\$target_path\" && ! $SUDO test -w \"\$parent_path\"; then echo \"ERROR: parent directory not writable: \$parent_path\"; fail=1; elif ! $SUDO test -d \"\$target_path\"; then echo \"INFO: directory will be created by deployment: \$target_path\"; else echo \"directory writable: \$target_path\"; fi;"
  done
  command_list+="if command -v ss >/dev/null 2>&1; then"
  for port_item in $port_list; do
    command_list+="if ss -ltnH | grep -Eq '[:.]${port_item}[[:space:]]'; then echo 'WARN: port ${port_item} is already listening (expected during upgrade)'; else echo 'port ${port_item} is available'; fi;"
  done
  command_list+="else echo 'WARN: ss command unavailable; port occupancy was not checked'; fi; exit \$fail"

  echo
  echo "Remote preflight: $(target_of "$host" "$user") ($name)"
  remote_exec "$host" "$user" "$port" "$command_list"
}

remote_health_check_host() {
  local name="$1" host="$2" user="$3" port="$4"
  local command_list="" port_list="" health_timeout health_interval
  health_timeout="${VARS[health_check_timeout_seconds]:-120}"
  health_interval="${VARS[health_check_interval_seconds]:-5}"
  if ! [[ "$health_timeout" =~ ^[0-9]+$ && "$health_interval" =~ ^[0-9]+$ && "$health_interval" -gt 0 ]]; then
    echo "Invalid health check timeout/interval configuration" >&2
    return 1
  fi
  case "$name" in
    postgresql)
      port_list="${VARS[postgresql_port]}"
      command_list="if ! $SUDO docker inspect -f '{{.State.Running}}' datamaster-postgresql 2>/dev/null | grep -q '^true$'; then echo 'ERROR: PostgreSQL container is not running'; fail=1; else if ! $SUDO docker exec datamaster-postgresql pg_isready -U $(sq "${VARS[postgresql_admin_user]}") -d $(sq "${VARS[postgresql_admin_database]}") >/dev/null 2>&1; then echo 'ERROR: PostgreSQL is not accepting connections'; fail=1; else echo 'PostgreSQL readiness: OK'; fi; fi;"
      ;;
    redis)
      port_list="${VARS[redis_port]}"
      if [[ -n "${VARS[redis_password]:-}" ]]; then
        command_list="if ! $SUDO docker inspect -f '{{.State.Running}}' datamaster-redis 2>/dev/null | grep -q '^true$'; then echo 'ERROR: Redis container is not running'; fail=1; else echo 'Redis container: running (password protected; PING skipped to avoid exposing credentials)'; fi;"
      else
        command_list="if ! $SUDO docker inspect -f '{{.State.Running}}' datamaster-redis 2>/dev/null | grep -q '^true$'; then echo 'ERROR: Redis container is not running'; fail=1; else if ! $SUDO docker exec datamaster-redis redis-cli ping 2>/dev/null | grep -q PONG; then echo 'ERROR: Redis did not answer PONG'; fail=1; else echo 'Redis readiness: OK'; fi; fi;"
      fi
      ;;
    dolphinscheduler)
      port_list="${VARS[dolphinscheduler_api_port]:-12345}"
      command_list="for unit in datamaster-dolphinscheduler-api datamaster-dolphinscheduler-master datamaster-dolphinscheduler-worker datamaster-dolphinscheduler-alert; do if ! $SUDO systemctl is-active --quiet \"\$unit\"; then echo \"ERROR: systemd unit \$unit is not active\"; fail=1; else echo \"\$unit: active\"; fi; done;"
      ;;
    dbgpt)
      port_list="${VARS[dbgpt_port]}"
      command_list="if ! $SUDO docker inspect -f '{{.State.Running}}' datamaster-dbgpt 2>/dev/null | grep -q '^true$'; then echo 'ERROR: DB-GPT container is not running'; fail=1; else echo 'DB-GPT container: running'; fi;"
      ;;
    datamaster_app)
      port_list="${VARS[datamaster_server_port]}"
      command_list="if ! $SUDO systemctl is-active --quiet datamaster-server; then echo 'ERROR: datamaster-server systemd unit is not active'; fail=1; else echo 'datamaster-server: active'; fi;"
      ;;
    nginx)
      port_list="${VARS[nginx_port]}"
      command_list="if ! $SUDO docker inspect -f '{{.State.Running}}' datamaster-nginx 2>/dev/null | grep -q '^true$'; then echo 'ERROR: Nginx container is not running'; fail=1; else echo 'Nginx container: running'; fi;"
      ;;
    *)
      echo "Unknown health-check host prefix: $name" >&2
      return 1
      ;;
  esac

  command_list="set -u; deadline=\$((\$(date +%s)+${health_timeout})); while true; do fail=0; $command_list"
  for port_item in $port_list; do
    command_list+="if ! command -v ss >/dev/null 2>&1; then echo 'ERROR: ss command not found; cannot verify port ${port_item}'; fail=1; elif ! ss -ltnH | grep -Eq '[:.]${port_item}[[:space:]]'; then echo 'ERROR: port ${port_item} is not listening'; fail=1; else echo 'port ${port_item}: listening'; fi;"
  done
  command_list+="if [ \"\$fail\" -eq 0 ]; then echo 'Health check: OK'; exit 0; fi; now=\$(date +%s); if [ \"\$now\" -ge \"\$deadline\" ]; then echo 'ERROR: health check timed out'; exit 1; fi; echo 'Health check not ready; retrying...'; sleep ${health_interval}; done"

  echo
  echo "Remote health check: $(target_of "$host" "$user") ($name)"
  remote_exec "$host" "$user" "$port" "$command_list"
}

render_template() {
  local template="$1" output="$2" content key source
  if [[ -f "$CONFIG_TEMPLATE_DIR/$template" ]]; then
    source="$CONFIG_TEMPLATE_DIR/$template"
  else
    source="$TEMPLATE_DIR/$template"
  fi
  content="$(cat "$source")"
  for key in "${VAR_KEYS[@]}"; do
    content="${content//"{{ $key }}"/${VARS[$key]}}"
  done
  printf '%s\n' "$content" > "$output"
}

render_temp() {
  local template="$1" tmp
  tmp="$(mktemp "${TMPDIR:-/tmp}/datamaster-render.XXXXXX")"
  render_template "$template" "$tmp"
  printf '%s' "$tmp"
}

install_remote_file() {
  local host="$1" user="$2" port="$3" local_path="$4" remote_path="$5" mode="${6:-0644}"
  local remote_dir target remote_tmp
  target="$(target_of "$host" "$user")"
  remote_dir="$(dirname "$remote_path")"
  backup_remote_target "$host" "$user" "$port" "$remote_path"
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "$remote_dir")"
  remote_tmp="$(remote_exec_capture "$host" "$user" "$port" "mktemp /tmp/datamaster-deploy-file.XXXXXX")"
  if [[ -z "$remote_tmp" ]]; then
    echo "Unable to create remote staging file on $target" >&2
    return 1
  fi
  echo "scp $local_path $target:$remote_tmp"
  if ! scp "${SSH_OPTS[@]}" -P "$port" "$local_path" "$target:$remote_tmp"; then
    remote_exec "$host" "$user" "$port" "rm -f $(sq "$remote_tmp")" || true
    return 1
  fi
  remote_exec "$host" "$user" "$port" "$SUDO install -m $mode $(sq "$remote_tmp") $(sq "$remote_path") && $SUDO rm -f $(sq "$remote_tmp")"
}

dir_has_payload() {
  local dir="$1"
  [[ -d "$dir" ]] && find "$dir" -mindepth 1 ! -name '.gitkeep' -print -quit | grep -q .
}

install_remote_dir() {
  local host="$1" user="$2" port="$3" local_dir="$4" remote_dir="$5"
  local remote_parent target remote_tmp
  target="$(target_of "$host" "$user")"
  remote_parent="$(dirname "$remote_dir")"
  backup_remote_target "$host" "$user" "$port" "$remote_dir"
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "$remote_parent")"
  remote_tmp="$(remote_exec_capture "$host" "$user" "$port" "mktemp -d /tmp/datamaster-deploy-dir.XXXXXX")"
  if [[ -z "$remote_tmp" ]]; then
    echo "Unable to create remote staging directory on $target" >&2
    return 1
  fi
  echo "scp -r $local_dir/. $target:$remote_tmp/"
  if ! scp "${SSH_OPTS[@]}" -P "$port" -r "$local_dir/." "$target:$remote_tmp/"; then
    remote_exec "$host" "$user" "$port" "rm -rf $(sq "$remote_tmp")" || true
    return 1
  fi
  remote_exec "$host" "$user" "$port" "$SUDO rm -rf $(sq "$remote_dir") && $SUDO mkdir -p $(sq "$remote_dir") && $SUDO cp -a $(sq "$remote_tmp")/. $(sq "$remote_dir")/ && rm -rf $(sq "$remote_tmp")"
}

resolve_local_path() {
  local path="$1"
  if [[ "$path" = /* ]]; then
    printf '%s' "$path"
  else
    (cd "$SCRIPT_DIR" && printf '%s/%s' "$(pwd)" "$path")
  fi
}

load_docker_image_if_present() {
  local host="$1" user="$2" port="$3" local_tar="$4" remote_name="$5" remote_path
  if [[ -z "$local_tar" || ! -f "$local_tar" ]]; then
    echo "Skip docker load: $local_tar not found"
    return 0
  fi
  remote_path="${VARS[remote_package_dir]}/$remote_name"
  install_remote_file "$host" "$user" "$port" "$local_tar" "$remote_path"
  remote_exec "$host" "$user" "$port" "$SUDO docker load -i $(sq "$remote_path")"
}

deploy_postgresql() {
  local name="$1" host="$2" user="$3" port="$4" image_tar
  echo
  echo "Deploying postgresql_servers to $(target_of "$host" "$user")..."
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "${VARS[postgresql_data_dir]}") $(sq "${VARS[postgresql_conf_dir]}") $(sq "${VARS[postgresql_log_dir]}")"
  image_tar="$(resolve_local_path "${VARS[postgresql_image_tar_src]}")"
  load_docker_image_if_present "$host" "$user" "$port" "$image_tar" "${VARS[postgresql_image_tar]}"
  remote_exec "$host" "$user" "$port" "$SUDO docker rm -f datamaster-postgresql >/dev/null 2>&1 || true; $SUDO docker run -d --name datamaster-postgresql --restart always -e TZ=$(sq "${VARS[timezone]}") -e POSTGRES_DB=$(sq "${VARS[postgresql_admin_database]}") -e POSTGRES_USER=$(sq "${VARS[postgresql_admin_user]}") -e POSTGRES_PASSWORD=$(sq "${VARS[postgresql_admin_password]}") -p ${VARS[postgresql_port]}:5432 -v $(sq "${VARS[postgresql_data_dir]}"):/var/lib/postgresql/data:Z -v $(sq "${VARS[postgresql_log_dir]}"):/var/log/postgresql:Z ${VARS[postgresql_image]}"
  deploy_database_init "$name" "$host" "$user" "$port"
}

deploy_database_init() {
  local name="$1" host="$2" user="$3" port="$4" package_dir sql_dir jar app_sql ds_sql app_upgrade_sql source upgrade_index
  local -a app_upgrade_sources
  package_dir="${VARS[remote_package_dir]}"
  sql_dir="${VARS[remote_init_sql_dir]}"
  jar="$(resolve_local_path "${VARS[database_init_jar_src]}")"
  app_sql="$(resolve_local_path "${VARS[postgresql_init_sql_src]}")"
  ds_sql="$(resolve_local_path "${VARS[dolphinscheduler_init_sql_src]}")"

  echo "Initializing PostgreSQL databases on $(target_of "$host" "$user")..."
  install_remote_file "$host" "$user" "$port" "$jar" "$package_dir/datamaster-db-init.jar"
  install_remote_file "$host" "$user" "$port" "$app_sql" "$sql_dir/datamaster.sql"
  install_remote_file "$host" "$user" "$port" "$ds_sql" "$sql_dir/dolphinscheduler.sql"
  remote_exec "$host" "$user" "$port" "until $SUDO docker exec datamaster-postgresql pg_isready -U $(sq "${VARS[postgresql_admin_user]}") -d $(sq "${VARS[postgresql_admin_database]}"); do sleep 2; done"
  remote_exec "$host" "$user" "$port" "java -jar $(sq "$package_dir/datamaster-db-init.jar") --host=${VARS[postgresql_ip]} --port=${VARS[postgresql_port]} --admin-db=${VARS[postgresql_admin_database]} --admin-user=${VARS[postgresql_admin_user]} --admin-password=${VARS[postgresql_admin_password]} --app-db=${VARS[postgresql_database]} --ds-db=${VARS[dolphinscheduler_database]} --app-user=${VARS[postgresql_user]} --app-password=${VARS[postgresql_password]} --app-sql=$(sq "$sql_dir/datamaster.sql") --ds-sql=$(sq "$sql_dir/dolphinscheduler.sql")"
  if [[ -n "${VARS[postgresql_app_upgrade_sql_src]:-}" ]]; then
    IFS=',' read -r -a app_upgrade_sources <<< "${VARS[postgresql_app_upgrade_sql_src]}"
    upgrade_index=0
    for source in "${app_upgrade_sources[@]}"; do
      source="${source#${source%%[![:space:]]*}}"
      source="${source%${source##*[![:space:]]}}"
      app_upgrade_sql="$(resolve_local_path "$source")"
      install_remote_file "$host" "$user" "$port" "$app_upgrade_sql" "$sql_dir/app-upgrade-${upgrade_index}.sql"
      execute_app_upgrade_sql "$host" "$user" "$port" "$sql_dir/app-upgrade-${upgrade_index}.sql"
      upgrade_index=$((upgrade_index + 1))
    done
  else
    echo "Skip DataMaster app upgrade SQL: postgresql_app_upgrade_sql_src is empty"
  fi
  apply_dolphinscheduler_compatibility_sql "$host" "$user" "$port"
  rewrite_dolphinscheduler_datamaster_urls "$host" "$user" "$port"
  upsert_dolphinscheduler_tenant "$host" "$user" "$port"
  upsert_dolphinscheduler_token "$host" "$user" "$port"
}

execute_app_upgrade_sql() {
  local host="$1" user="$2" port="$3" remote_sql="$4"
  echo "Applying DataMaster app upgrade SQL: $remote_sql"
  execute_postgresql_sql_file "$host" "$user" "$port" "${VARS[postgresql_database]}" "$remote_sql" "datamaster-app-upgrade.sql"
}

execute_postgresql_sql_file() {
  local host="$1" user="$2" port="$3" database="$4" remote_sql="$5" container_name="$6"
  remote_exec "$host" "$user" "$port" "$SUDO docker cp $(sq "$remote_sql") datamaster-postgresql:/tmp/$(basename "$container_name") && $SUDO docker exec -e PGPASSWORD=$(sq "${VARS[postgresql_password]}") datamaster-postgresql psql -v ON_ERROR_STOP=1 -U $(sq "${VARS[postgresql_user]}") -d $(sq "$database") -f /tmp/$(basename "$container_name")"
}

execute_postgresql_sql_text() {
  local host="$1" user="$2" port="$3" database="$4" label="$5" sql="$6" tmp remote_sql remote_name
  tmp="$(mktemp "${TMPDIR:-/tmp}/datamaster-sql.XXXXXX.sql")"
  remote_name="$label.sql"
  remote_sql="${VARS[remote_init_sql_dir]}/$remote_name"
  printf '%s
' "$sql" > "$tmp"
  install_remote_file "$host" "$user" "$port" "$tmp" "$remote_sql"
  rm -f "$tmp"
  execute_postgresql_sql_file "$host" "$user" "$port" "$database" "$remote_sql" "$remote_name"
}

apply_dolphinscheduler_compatibility_sql() {
  local host="$1" user="$2" port="$3" sql
  sql="ALTER TABLE public.t_ds_project_parameter ADD COLUMN IF NOT EXISTS param_data_type varchar(50) DEFAULT 'VARCHAR';"
  echo "Applying DolphinScheduler compatibility SQL to ${VARS[dolphinscheduler_database]}..."
  execute_postgresql_sql_text "$host" "$user" "$port" "${VARS[dolphinscheduler_database]}" "dolphinscheduler-compatibility" "$sql"
}

rewrite_dolphinscheduler_datamaster_urls() {
  local host="$1" user="$2" port="$3" target_url target_sql sql
  target_url="http://${VARS[datamaster_app_ip]}:${VARS[datamaster_server_port]}"
  target_sql="$(sql_literal "$target_url")"
  sql="DO \$\$ BEGIN
UPDATE public.t_ds_task_definition
   SET task_params = regexp_replace(task_params, 'http://[^/]+:8080', $target_sql, 'g')
 WHERE task_params LIKE '%/col/%' OR task_params LIKE '%/quality/%' OR task_params LIKE '%/cat/%';
UPDATE public.t_ds_task_definition_log
   SET task_params = regexp_replace(task_params, 'http://[^/]+:8080', $target_sql, 'g')
 WHERE task_params LIKE '%/col/%' OR task_params LIKE '%/quality/%' OR task_params LIKE '%/cat/%';
UPDATE public.t_ds_task_instance
   SET task_params = regexp_replace(task_params, 'http://[^/]+:8080', $target_sql, 'g')
 WHERE task_params LIKE '%/col/%' OR task_params LIKE '%/quality/%' OR task_params LIKE '%/cat/%';
END \$\$;"
  echo "Rewriting DolphinScheduler DataMaster callback URLs to ${target_url}..."
  execute_postgresql_sql_text "$host" "$user" "$port" "${VARS[dolphinscheduler_database]}" "dolphinscheduler-datamaster-url" "$sql"
}

upsert_dolphinscheduler_tenant() {
  local host="$1" user="$2" port="$3" tenant_sql sql
  tenant_sql="$(sql_literal "${VARS[dolphinscheduler_tenant_code]}")"
  sql="DO \$\$ BEGIN IF EXISTS (SELECT 1 FROM public.t_ds_tenant WHERE tenant_code = $tenant_sql) THEN UPDATE public.t_ds_tenant SET description = 'DataMaster deployment tenant', queue_id = 1, update_time = now() WHERE tenant_code = $tenant_sql; ELSE INSERT INTO public.t_ds_tenant (tenant_code, description, queue_id, create_time, update_time) VALUES ($tenant_sql, 'DataMaster deployment tenant', 1, now(), now()); END IF; END \$\$;"
  echo "Ensuring DolphinScheduler tenant ${VARS[dolphinscheduler_tenant_code]} in ${VARS[dolphinscheduler_database]}..."
  execute_postgresql_sql_text "$host" "$user" "$port" "${VARS[dolphinscheduler_database]}" "dolphinscheduler-tenant" "$sql"
}

upsert_dolphinscheduler_token() {
  local host="$1" user="$2" port="$3" token_sql sql
  token_sql="$(sql_literal "${VARS[dolphinscheduler_token]}")"
  sql="DO \$\$ DECLARE admin_id integer; BEGIN SELECT id INTO admin_id FROM public.t_ds_user WHERE user_name = 'admin' ORDER BY id LIMIT 1; IF admin_id IS NULL THEN RAISE EXCEPTION 'DolphinScheduler admin user not found'; END IF; IF EXISTS (SELECT 1 FROM public.t_ds_access_token WHERE user_id = admin_id) THEN UPDATE public.t_ds_access_token SET token = $token_sql, expire_time = timestamp '2099-12-31 23:59:59', update_time = now() WHERE user_id = admin_id; ELSE INSERT INTO public.t_ds_access_token (user_id, token, expire_time, create_time, update_time) VALUES (admin_id, $token_sql, timestamp '2099-12-31 23:59:59', now(), now()); END IF; END \$\$;"
  echo "Writing DolphinScheduler API token into ${VARS[dolphinscheduler_database]}..."
  execute_postgresql_sql_text "$host" "$user" "$port" "${VARS[dolphinscheduler_database]}" "dolphinscheduler-token" "$sql"
}

deploy_redis() {
  local name="$1" host="$2" user="$3" port="$4" conf image_tar
  echo
  echo "Deploying redis_servers to $(target_of "$host" "$user")..."
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "${VARS[redis_data_dir]}") $(sq "${VARS[redis_conf_dir]}") $(sq "${VARS[redis_log_dir]}")"
  image_tar="$(resolve_local_path "${VARS[redis_image_tar_src]}")"
  load_docker_image_if_present "$host" "$user" "$port" "$image_tar" "${VARS[redis_image_tar]}"
  conf="$(render_temp redis.conf.j2)"
  install_remote_file "$host" "$user" "$port" "$conf" "${VARS[redis_conf_dir]}/redis.conf"
  remote_exec "$host" "$user" "$port" "$SUDO docker rm -f datamaster-redis >/dev/null 2>&1 || true; $SUDO docker run -d --name datamaster-redis --restart always -e TZ=$(sq "${VARS[timezone]}") -p ${VARS[redis_port]}:6379 -v $(sq "${VARS[redis_data_dir]}"):/data:Z -v $(sq "${VARS[redis_conf_dir]}/redis.conf"):/etc/redis/redis.conf:ro,Z -v $(sq "${VARS[redis_log_dir]}"):/logs:Z ${VARS[redis_image]} redis-server /etc/redis/redis.conf"
}

deploy_dolphinscheduler() {
  local name="$1" host="$2" user="$3" port="$4" dir script package soft_dir chunjun_dir flink_dir
  dir="${VARS[dolphinscheduler_dir]}"
  echo
  echo "Deploying dolphinscheduler_servers to $(target_of "$host" "$user")..."
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "${VARS[dolphinscheduler_log_dir]}") $(sq "${VARS[dolphinscheduler_conf_dir]}") $(sq "${VARS[dolphinscheduler_resource_dir]}") $(sq "${VARS[remote_soft_dir]}") $(sq "${VARS[remote_package_dir]}")"
  package="$(resolve_local_path "${VARS[dolphinscheduler_package_src]}")"
  if [[ -f "$package" ]]; then
    install_remote_file "$host" "$user" "$port" "$package" "${VARS[remote_package_dir]}/${VARS[dolphinscheduler_install_tgz]}"
  fi
  soft_dir="$(resolve_local_path "${VARS[soft_package_src]}")"
  chunjun_dir="$soft_dir/chunjun"
  flink_dir="$soft_dir/flink"
  if dir_has_payload "$chunjun_dir"; then
    install_remote_dir "$host" "$user" "$port" "$chunjun_dir" "${VARS[chunjun_home]}"
    remote_exec "$host" "$user" "$port" "$SUDO chmod -R a+rX $(sq "${VARS[chunjun_home]}") && if [ -d $(sq "${VARS[chunjun_home]}/bin") ]; then $SUDO chmod -R a+rx $(sq "${VARS[chunjun_home]}/bin"); fi"
  else
    echo "Skip Chunjun upload: $chunjun_dir has no payload files"
  fi
  if dir_has_payload "$flink_dir"; then
    install_remote_dir "$host" "$user" "$port" "$flink_dir" "${VARS[flink_home]}"
  else
    echo "Skip Flink upload: $flink_dir has no payload files"
  fi
  script="$(render_temp install-dolphinscheduler.sh.j2)"
  install_remote_file "$host" "$user" "$port" "$script" "$dir/install-dolphinscheduler.sh" "0755"
  remote_exec "$host" "$user" "$port" "$SUDO bash $(sq "$dir/install-dolphinscheduler.sh")"
}

deploy_dbgpt() {
  local name="$1" host="$2" user="$3" port="$4" image_tar
  echo
  echo "Deploying dbgpt_servers to $(target_of "$host" "$user")..."
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "${VARS[dbgpt_data_dir]}") $(sq "${VARS[dbgpt_message_dir]}")"
  image_tar="$(resolve_local_path "${VARS[dbgpt_image_tar_src]}")"
  load_docker_image_if_present "$host" "$user" "$port" "$image_tar" "${VARS[dbgpt_image_tar]}"
  remote_exec "$host" "$user" "$port" "$SUDO docker rm -f datamaster-dbgpt >/dev/null 2>&1 || true; $SUDO docker run -d --name datamaster-dbgpt --restart always --security-opt seccomp=unconfined -e TZ=$(sq "${VARS[timezone]}") -e LOCAL_DB_TYPE=sqlite -e LOCAL_DB_PATH=/app/pilot/data/default_sqlite.db -e LLM_MODEL=proxyllm -e LANGUAGE=zh -e DASHSCOPE_API_KEY=$(sq "${VARS[dashscope_api_key]}") -e OPENBLAS_NUM_THREADS=1 -e OMP_NUM_THREADS=1 -e MKL_NUM_THREADS=1 -e NUMEXPR_NUM_THREADS=1 -p ${VARS[dbgpt_port]}:5670 -v $(sq "${VARS[dbgpt_data_dir]}"):/app/pilot/data:Z -v $(sq "${VARS[dbgpt_message_dir]}"):/app/pilot/message:Z ${VARS[dbgpt_image]} sh -c $(sq "pip install psycopg2-binary -q && dbgpt start webserver --config /app/configs/dbgpt-proxy-tongyi.toml")"
}

deploy_app() {
  local name="$1" host="$2" user="$3" port="$4" app unit jar
  echo
  echo "Deploying datamaster_app_servers to $(target_of "$host" "$user") with java -jar..."
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "${VARS[app_conf_dir]}") $(sq "${VARS[app_log_dir]}") $(sq "${VARS[app_upload_dir]}")"
  jar="$(resolve_local_path "${VARS[datamaster_server_jar_src]}")"
  install_remote_file "$host" "$user" "$port" "$jar" "${VARS[app_jar_path]}" "0755"
  app="$(render_temp datamaster-server-application-prod.yml.j2)"
  install_remote_file "$host" "$user" "$port" "$app" "${VARS[app_conf_dir]}/application-prod.yml"
  unit="$(render_temp datamaster-server.service.j2)"
  install_remote_file "$host" "$user" "$port" "$unit" "/etc/systemd/system/datamaster-server.service"
  remote_exec "$host" "$user" "$port" "$SUDO docker rm -f datamaster-server >/dev/null 2>&1 || true; $SUDO systemctl daemon-reload && $SUDO systemctl enable datamaster-server && $SUDO systemctl restart datamaster-server"
}

deploy_nginx() {
  local name="$1" host="$2" user="$3" port="$4" conf image_tar ui_dist
  echo
  echo "Deploying nginx_servers to $(target_of "$host" "$user")..."
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "${VARS[nginx_conf_dir]}") $(sq "${VARS[nginx_html_dir]}") $(sq "${VARS[nginx_log_dir]}")"
  image_tar="$(resolve_local_path "${VARS[nginx_image_tar_src]}")"
  load_docker_image_if_present "$host" "$user" "$port" "$image_tar" "${VARS[nginx_image_tar]}"
  conf="$(render_temp nginx.conf.j2)"
  install_remote_file "$host" "$user" "$port" "$conf" "${VARS[nginx_conf_dir]}/nginx.conf"
  ui_dist="$(resolve_local_path "${VARS[datamaster_ui_dist_src]}")"
  install_remote_dir "$host" "$user" "$port" "$ui_dist" "${VARS[nginx_html_dir]}"
  remote_exec "$host" "$user" "$port" "$SUDO docker rm -f datamaster-nginx >/dev/null 2>&1 || true; $SUDO docker run -d --name datamaster-nginx --restart always -e TZ=$(sq "${VARS[timezone]}") -p ${VARS[nginx_port]}:80 -v $(sq "${VARS[nginx_conf_dir]}/nginx.conf"):/etc/nginx/nginx.conf:ro,Z -v $(sq "${VARS[nginx_html_dir]}"):/usr/share/nginx/html:ro,Z -v $(sq "${VARS[nginx_log_dir]}"):/var/log/nginx:Z ${VARS[nginx_image]}"
}

load_vars
ensure_runtime_vars
load_hosts
derive_service_vars
validate_config

if [[ "$REMOTE_CHECK_ONLY" -eq 1 ]]; then
  for_each_host postgresql_servers remote_check_host
  for_each_host redis_servers remote_check_host
  for_each_host dolphinscheduler_servers remote_check_host
  for_each_host dbgpt_servers remote_check_host
  for_each_host datamaster_app_servers remote_check_host
  for_each_host nginx_servers remote_check_host
  echo "Remote deploy preflight passed: $CONFIG_FILE"
  exit 0
fi

if [[ "$HEALTH_CHECK_ONLY" -eq 1 ]]; then
  for_each_host postgresql_servers remote_health_check_host
  for_each_host redis_servers remote_health_check_host
  for_each_host dolphinscheduler_servers remote_health_check_host
  for_each_host dbgpt_servers remote_health_check_host
  for_each_host datamaster_app_servers remote_health_check_host
  for_each_host nginx_servers remote_health_check_host
  echo "Remote health check passed: $CONFIG_FILE"
  exit 0
fi

if [[ "$CHECK_ONLY" -eq 1 ]]; then
  echo "Deploy config check passed: $CONFIG_FILE"
  exit 0
fi

for_each_host postgresql_servers deploy_postgresql
for_each_host redis_servers deploy_redis
for_each_host dolphinscheduler_servers deploy_dolphinscheduler
for_each_host dbgpt_servers deploy_dbgpt
for_each_host datamaster_app_servers deploy_app
for_each_host nginx_servers deploy_nginx

if [[ "$POST_HEALTH_CHECK" -eq 1 ]]; then
  for_each_host postgresql_servers remote_health_check_host
  for_each_host redis_servers remote_health_check_host
  for_each_host dolphinscheduler_servers remote_health_check_host
  for_each_host dbgpt_servers remote_health_check_host
  for_each_host datamaster_app_servers remote_health_check_host
  for_each_host nginx_servers remote_health_check_host
  echo "Post-deploy health check passed: $CONFIG_FILE"
fi
