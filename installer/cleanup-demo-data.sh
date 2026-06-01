#!/bin/bash
set -euo pipefail

CURRENT_DIR=$(
  cd "$(dirname "$0")"
  pwd
)

ASSUME_YES=false
if [[ "${1:-}" == "--yes" || "${1:-}" == "-y" ]]; then
  ASSUME_YES=true
fi

if [[ -f "${CURRENT_DIR}/install.conf" ]]; then
  set -a
  source "${CURRENT_DIR}/install.conf"
  set +a
fi

load_env_file() {
  local env_file=$1
  local key value
  while IFS='=' read -r key value || [[ -n "$key" ]]; do
    [[ "$key" =~ ^CREST_[A-Z0-9_]+$ ]] || continue
    export "${key}=${value}"
  done < "$env_file"
}

if [[ -f /usr/bin/crestctl ]]; then
  installed_base=$(grep "^CREST_BASE=" /usr/bin/crestctl | cut -d'=' -f2)
  if [[ -n "${installed_base}" ]]; then
    CREST_BASE="${installed_base}"
  fi
fi

CREST_BASE=${CREST_BASE:-/opt}
CREST_RUN_BASE=${CREST_RUN_BASE:-${CREST_BASE}/crest}

if [[ -f "${CREST_RUN_BASE}/.env" ]]; then
  load_env_file "${CREST_RUN_BASE}/.env"
fi

CREST_DB_HOST=${CREST_DB_HOST:-mysql-crest}
CREST_DB_PORT=${CREST_DB_PORT:-3306}
CREST_DB_NAME=${CREST_DB_NAME:-crest}
CREST_DB_USERNAME=${CREST_DB_USERNAME:-root}
CREST_MYSQL_IMAGE=${CREST_MYSQL_IMAGE:-mysql:8.0.43}
CREST_EXTERNAL_MYSQL=${CREST_EXTERNAL_MYSQL:-false}

SQL_FILE="${CURRENT_DIR}/crest/bin/manual/cleanup-demo-data.sql"
if [[ ! -f "${SQL_FILE}" && -f "${CREST_RUN_BASE}/bin/manual/cleanup-demo-data.sql" ]]; then
  SQL_FILE="${CREST_RUN_BASE}/bin/manual/cleanup-demo-data.sql"
fi

if [[ ! -f "${SQL_FILE}" ]]; then
  echo "未找到 cleanup-demo-data.sql"
  exit 1
fi

if [[ "${ASSUME_YES}" != "true" ]]; then
  echo "该操作会删除 Crest 内置演示资源，包括演示数据源、数据集、图表、大屏和演示库。"
  read -r -p "确认继续？输入 yes 后执行: " confirm
  if [[ "${confirm}" != "yes" ]]; then
    echo "已取消"
    exit 0
  fi
fi

was_running=false
if docker ps --format '{{.Names}}' | grep -qx 'crest'; then
  was_running=true
  echo "停止 Crest 应用容器"
  docker stop crest >/dev/null
fi

disable_demo_runtime_config() {
  if [[ -f "${CREST_RUN_BASE}/.env" ]]; then
    if grep -q '^CREST_LOAD_DEMO=' "${CREST_RUN_BASE}/.env"; then
      sed -i.bak 's#^CREST_LOAD_DEMO=.*#CREST_LOAD_DEMO=false#g' "${CREST_RUN_BASE}/.env"
    else
      echo 'CREST_LOAD_DEMO=false' >> "${CREST_RUN_BASE}/.env"
    fi
    if grep -q '^CREST_FLYWAY_LOCATIONS=' "${CREST_RUN_BASE}/.env"; then
      sed -i.bak 's#^CREST_FLYWAY_LOCATIONS=.*#CREST_FLYWAY_LOCATIONS=classpath:db/migration#g' "${CREST_RUN_BASE}/.env"
    else
      echo 'CREST_FLYWAY_LOCATIONS=classpath:db/migration' >> "${CREST_RUN_BASE}/.env"
    fi
    rm -f "${CREST_RUN_BASE}/.env.bak"
  fi

  if [[ -f "${CREST_RUN_BASE}/conf/application.yml" ]]; then
    tmp_config=$(mktemp)
    awk '
      /^[[:space:]]*demo:[[:space:]]*$/ { in_demo = 1; print; next }
      in_demo && /^[[:space:]]*enabled:[[:space:]]*/ { sub(/enabled:.*/, "enabled: false"); in_demo = 0; print; next }
      in_demo && /^[^[:space:]]/ { in_demo = 0 }
      { print }
    ' "${CREST_RUN_BASE}/conf/application.yml" > "${tmp_config}"
    mv "${tmp_config}" "${CREST_RUN_BASE}/conf/application.yml"
    sed -i.bak 's#locations: classpath:db/migration,classpath:db/demo-migration#locations: classpath:db/migration#g' "${CREST_RUN_BASE}/conf/application.yml"
    rm -f "${CREST_RUN_BASE}/conf/application.yml.bak"
  fi
}

mysql_password_arg=()
if [[ -n "${CREST_DB_PASSWORD:-}" ]]; then
  mysql_password_arg=("-p${CREST_DB_PASSWORD}")
fi

run_sql_with_internal_mysql() {
  if ! docker ps --format '{{.Names}}' | grep -qx 'mysql-crest'; then
    echo "未找到 mysql-crest 容器"
    exit 1
  fi
  docker exec -i mysql-crest mysql \
    -u"${CREST_DB_USERNAME}" "${mysql_password_arg[@]}" "${CREST_DB_NAME}" < "${SQL_FILE}"
}

run_sql_with_external_mysql() {
  if command -v mysql >/dev/null 2>&1; then
    mysql -h"${CREST_DB_HOST}" -P"${CREST_DB_PORT}" \
      -u"${CREST_DB_USERNAME}" "${mysql_password_arg[@]}" "${CREST_DB_NAME}" < "${SQL_FILE}"
    return
  fi
  docker run --rm -i --network host "${CREST_MYSQL_IMAGE}" mysql \
    -h"${CREST_DB_HOST}" -P"${CREST_DB_PORT}" \
    -u"${CREST_DB_USERNAME}" "${mysql_password_arg[@]}" "${CREST_DB_NAME}" < "${SQL_FILE}"
}

disable_demo_runtime_config

echo "清理内置演示资源"
if [[ "${CREST_EXTERNAL_MYSQL}" == "true" ]]; then
  run_sql_with_external_mysql
else
  run_sql_with_internal_mysql
fi

if [[ "${was_running}" == "true" ]]; then
  echo "启动 Crest 应用容器"
  if command -v crestctl >/dev/null 2>&1; then
    crestctl start
  else
    docker start crest >/dev/null
  fi
fi

echo "演示资源已清理，后续启动不会再初始化内置演示资源。"
