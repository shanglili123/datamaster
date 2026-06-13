#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATE_DIR="$SCRIPT_DIR/templates"
CONFIG_FILE="$SCRIPT_DIR/deploy.yml"
LIMIT=""
SUDO="sudo"

usage() {
  cat >&2 <<'EOF'
Usage: ./deploy/start-all.sh [--config deploy.yml] [--limit group[,group]] [--sudo sudo]
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

  local i k ref
  for ((i = 0; i < 8; i++)); do
    for k in "${VAR_KEYS[@]}"; do
      for ref in "${VAR_KEYS[@]}"; do
        VARS["$k"]="${VARS[$k]//"{{ $ref }}"/${VARS[$ref]}}"
      done
    done
  done
}

add_host_from_config() {
  local group="$1" prefix="$2" host user port
  host="${VARS[${prefix}_ssh_host]:-}"
  user="${VARS[${prefix}_ssh_user]:-}"
  port="${VARS[${prefix}_ssh_port]:-22}"
  if [[ -z "$host" ]]; then
    echo "Missing ${prefix}_ssh_host in $CONFIG_FILE" >&2
    exit 1
  fi
  HOST_ROWS+=("$group|$prefix|$host|$user|$port")
}

load_hosts() {
  add_host_from_config postgresql_servers postgresql
  add_host_from_config redis_servers redis
  add_host_from_config zookeeper_servers zookeeper
  add_host_from_config dolphinscheduler_servers dolphinscheduler
  add_host_from_config datamaster_app_servers datamaster_app
  add_host_from_config datamaster_quality_servers datamaster_quality
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
  if [[ -z "${VARS[dolphinscheduler_token]:-}" || "${VARS[dolphinscheduler_token]}" == "<DOLPHINSCHEDULER_TOKEN>" ]]; then
    if [[ -f "$token_file" ]]; then
      token="$(tr -d '\r\n' < "$token_file")"
    else
      mkdir -p "$(dirname "$token_file")"
      token="$(generate_token)"
      printf '%s\n' "$token" > "$token_file"
      chmod 600 "$token_file" 2>/dev/null || true
    fi
    VARS[dolphinscheduler_token]="$token"
    echo "Using generated DolphinScheduler token from $token_file"
  fi
}

remote_exec() {
  local host="$1" user="$2" port="$3" command="$4" target
  target="$(target_of "$host" "$user")"
  echo "ssh -p $port $target $command"
  ssh -p "$port" "$target" "$command"
}

remote_upload_temp() {
  local host="$1" user="$2" port="$3" local_path="$4" remote_temp="$5" target
  target="$(target_of "$host" "$user")"
  remote_exec "$host" "$user" "$port" "mkdir -p /tmp/datamaster-deploy"
  echo "scp $local_path $target:$remote_temp"
  scp -P "$port" "$local_path" "$target:$remote_temp"
}

render_template() {
  local template="$1" output="$2" content key
  content="$(cat "$TEMPLATE_DIR/$template")"
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
  local remote_temp remote_dir
  remote_temp="/tmp/datamaster-deploy/$(basename "$remote_path").$$"
  remote_dir="$(dirname "$remote_path")"
  remote_upload_temp "$host" "$user" "$port" "$local_path" "$remote_temp"
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "$remote_dir") && $SUDO cp $(sq "$remote_temp") $(sq "$remote_path") && $SUDO chmod $mode $(sq "$remote_path")"
}

dir_has_payload() {
  local dir="$1"
  [[ -d "$dir" ]] && find "$dir" -mindepth 1 ! -name '.gitkeep' -print -quit | grep -q .
}

install_remote_dir() {
  local host="$1" user="$2" port="$3" local_dir="$4" remote_dir="$5"
  local remote_temp remote_parent target
  target="$(target_of "$host" "$user")"
  remote_temp="/tmp/datamaster-deploy/$(basename "$remote_dir").$$"
  remote_parent="$(dirname "$remote_dir")"
  remote_exec "$host" "$user" "$port" "rm -rf $(sq "$remote_temp") && mkdir -p /tmp/datamaster-deploy"
  echo "scp -r $local_dir $target:$remote_temp"
  scp -P "$port" -r "$local_dir" "$target:$remote_temp"
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "$remote_parent") && $SUDO rm -rf $(sq "$remote_dir") && $SUDO cp -a $(sq "$remote_temp") $(sq "$remote_dir")"
}

resolve_local_path() {
  local path="$1"
  if [[ "$path" = /* ]]; then
    printf '%s' "$path"
  else
    (cd "$SCRIPT_DIR" && cd "$(dirname "$path")" && printf '%s/%s' "$(pwd)" "$(basename "$path")")
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
  remote_exec "$host" "$user" "$port" "$SUDO docker rm -f datamaster-postgresql >/dev/null 2>&1 || true; $SUDO docker run -d --name datamaster-postgresql --restart always -e TZ=$(sq "${VARS[timezone]}") -e POSTGRES_DB=$(sq "${VARS[postgresql_admin_database]}") -e POSTGRES_USER=$(sq "${VARS[postgresql_admin_user]}") -e POSTGRES_PASSWORD=$(sq "${VARS[postgresql_admin_password]}") -p ${VARS[postgresql_port]}:5432 -v $(sq "${VARS[postgresql_data_dir]}"):/var/lib/postgresql/data -v $(sq "${VARS[postgresql_log_dir]}"):/var/log/postgresql ${VARS[postgresql_image]}"
  deploy_database_init "$name" "$host" "$user" "$port"
}

deploy_database_init() {
  local name="$1" host="$2" user="$3" port="$4" package_dir sql_dir jar app_sql ds_sql
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
  upsert_dolphinscheduler_tenant "$host" "$user" "$port"
  upsert_dolphinscheduler_token "$host" "$user" "$port"
}

upsert_dolphinscheduler_tenant() {
  local host="$1" user="$2" port="$3" tenant_sql sql
  tenant_sql="$(sql_literal "${VARS[dolphinscheduler_tenant_code]}")"
  sql="DO \$\$ BEGIN IF EXISTS (SELECT 1 FROM public.t_ds_tenant WHERE tenant_code = $tenant_sql) THEN UPDATE public.t_ds_tenant SET description = 'DataMaster deployment tenant', queue_id = 1, update_time = now() WHERE tenant_code = $tenant_sql; ELSE INSERT INTO public.t_ds_tenant (tenant_code, description, queue_id, create_time, update_time) VALUES ($tenant_sql, 'DataMaster deployment tenant', 1, now(), now()); END IF; END \$\$;"
  echo "Ensuring DolphinScheduler tenant ${VARS[dolphinscheduler_tenant_code]} in ${VARS[dolphinscheduler_database]}..."
  remote_exec "$host" "$user" "$port" "$SUDO docker exec -e PGPASSWORD=$(sq "${VARS[postgresql_password]}") datamaster-postgresql psql -v ON_ERROR_STOP=1 -U $(sq "${VARS[postgresql_user]}") -d $(sq "${VARS[dolphinscheduler_database]}") -c $(sq "$sql")"
}

upsert_dolphinscheduler_token() {
  local host="$1" user="$2" port="$3" token_sql sql
  token_sql="$(sql_literal "${VARS[dolphinscheduler_token]}")"
  sql="DO \$\$ DECLARE admin_id integer; BEGIN SELECT id INTO admin_id FROM public.t_ds_user WHERE user_name = 'admin' ORDER BY id LIMIT 1; IF admin_id IS NULL THEN RAISE EXCEPTION 'DolphinScheduler admin user not found'; END IF; IF EXISTS (SELECT 1 FROM public.t_ds_access_token WHERE user_id = admin_id) THEN UPDATE public.t_ds_access_token SET token = $token_sql, expire_time = timestamp '2099-12-31 23:59:59', update_time = now() WHERE user_id = admin_id; ELSE INSERT INTO public.t_ds_access_token (user_id, token, expire_time, create_time, update_time) VALUES (admin_id, $token_sql, timestamp '2099-12-31 23:59:59', now(), now()); END IF; END \$\$;"
  echo "Writing DolphinScheduler API token into ${VARS[dolphinscheduler_database]}..."
  remote_exec "$host" "$user" "$port" "$SUDO docker exec -e PGPASSWORD=$(sq "${VARS[postgresql_password]}") datamaster-postgresql psql -v ON_ERROR_STOP=1 -U $(sq "${VARS[postgresql_user]}") -d $(sq "${VARS[dolphinscheduler_database]}") -c $(sq "$sql")"
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
  remote_exec "$host" "$user" "$port" "$SUDO docker rm -f datamaster-redis >/dev/null 2>&1 || true; $SUDO docker run -d --name datamaster-redis --restart always -e TZ=$(sq "${VARS[timezone]}") -p ${VARS[redis_port]}:6379 -v $(sq "${VARS[redis_data_dir]}"):/data -v $(sq "${VARS[redis_conf_dir]}/redis.conf"):/etc/redis/redis.conf:ro -v $(sq "${VARS[redis_log_dir]}"):/logs ${VARS[redis_image]} redis-server /etc/redis/redis.conf"
}

deploy_zookeeper() {
  local name="$1" host="$2" user="$3" port="$4" dir script package
  dir="${VARS[zookeeper_dir]}"
  echo
  echo "Deploying zookeeper_servers to $(target_of "$host" "$user")..."
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "${VARS[zookeeper_data_dir]}") $(sq "${VARS[zookeeper_log_dir]}") $(sq "${VARS[remote_package_dir]}")"
  package="$(resolve_local_path "${VARS[zookeeper_package_src]}")"
  if [[ -f "$package" ]]; then
    install_remote_file "$host" "$user" "$port" "$package" "${VARS[remote_package_dir]}/${VARS[zookeeper_install_tgz]}"
  fi
  script="$(render_temp install-zookeeper.sh.j2)"
  install_remote_file "$host" "$user" "$port" "$script" "$dir/install-zookeeper.sh" "0755"
  remote_exec "$host" "$user" "$port" "$SUDO bash $(sq "$dir/install-zookeeper.sh")"
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

deploy_app() {
  local name="$1" host="$2" user="$3" port="$4" app image_tar
  echo
  echo "Deploying datamaster_app_servers to $(target_of "$host" "$user")..."
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "${VARS[app_conf_dir]}") $(sq "${VARS[app_log_dir]}") $(sq "${VARS[app_upload_dir]}")"
  image_tar="$(resolve_local_path "${VARS[datamaster_server_image_tar_src]}")"
  load_docker_image_if_present "$host" "$user" "$port" "$image_tar" "${VARS[datamaster_server_image_tar]}"
  app="$(render_temp datamaster-server-application-prod.yml.j2)"
  install_remote_file "$host" "$user" "$port" "$app" "${VARS[app_conf_dir]}/application-prod.yml"
  remote_exec "$host" "$user" "$port" "$SUDO docker rm -f datamaster-server >/dev/null 2>&1 || true; $SUDO docker run -d --name datamaster-server --restart always -e TZ=$(sq "${VARS[timezone]}") -e SPRING_PROFILES_ACTIVE=prod -e DS_INCREMENTAL_PREPARE_URL=$(sq "http://${VARS[datamaster_app_ip]}:${VARS[datamaster_server_port]}/col/etlTask/incremental/prepare") -e DS_INCREMENTAL_COMPLETE_URL=$(sq "http://${VARS[datamaster_app_ip]}:${VARS[datamaster_server_port]}/col/etlTask/incremental/complete") -p ${VARS[datamaster_server_port]}:8080 -v $(sq "${VARS[app_conf_dir]}/application-prod.yml"):/usr/app/jar/application-prod.yml:ro -v $(sq "${VARS[app_log_dir]}"):/usr/app/jar/logs -v $(sq "${VARS[app_upload_dir]}"):/usr/app/jar/upload --add-host postgresql:${VARS[postgresql_ip]} --add-host redis:${VARS[redis_ip]} --add-host dolphinscheduler:${VARS[dolphinscheduler_ip]} ${VARS[datamaster_server_image]}"
}

deploy_quality() {
  local name="$1" host="$2" user="$3" port="$4" app image_tar
  echo
  echo "Deploying datamaster_quality_servers to $(target_of "$host" "$user")..."
  remote_exec "$host" "$user" "$port" "$SUDO mkdir -p $(sq "${VARS[quality_conf_dir]}") $(sq "${VARS[quality_log_dir]}") $(sq "${VARS[quality_job_log_dir]}")"
  image_tar="$(resolve_local_path "${VARS[datamaster_quality_image_tar_src]}")"
  load_docker_image_if_present "$host" "$user" "$port" "$image_tar" "${VARS[datamaster_quality_image_tar]}"
  app="$(render_temp datamaster-quality-application-prod.yml.j2)"
  install_remote_file "$host" "$user" "$port" "$app" "${VARS[quality_conf_dir]}/application-prod.yml"
  remote_exec "$host" "$user" "$port" "$SUDO docker rm -f datamaster-quality >/dev/null 2>&1 || true; $SUDO docker run -d --name datamaster-quality --restart always -e TZ=$(sq "${VARS[timezone]}") -e SPRING_PROFILES_ACTIVE=prod -p ${VARS[datamaster_quality_port]}:8083 -v $(sq "${VARS[quality_conf_dir]}/application-prod.yml"):/usr/app/jar/application-prod.yml:ro -v $(sq "${VARS[quality_log_dir]}"):/usr/app/jar/logs -v $(sq "${VARS[quality_job_log_dir]}"):/usr/app/jar/job-log --add-host postgresql:${VARS[postgresql_ip]} --add-host redis:${VARS[redis_ip]} ${VARS[datamaster_quality_image]}"
}

load_vars
ensure_runtime_vars
load_hosts

for_each_host postgresql_servers deploy_postgresql
for_each_host redis_servers deploy_redis
for_each_host zookeeper_servers deploy_zookeeper
for_each_host dolphinscheduler_servers deploy_dolphinscheduler
for_each_host datamaster_app_servers deploy_app
for_each_host datamaster_quality_servers deploy_quality
