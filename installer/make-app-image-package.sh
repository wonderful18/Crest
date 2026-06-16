#!/bin/bash
set -euo pipefail

INSTALLER_DIR=$(
  cd "$(dirname "$0")"
  pwd
)
REPO_DIR=$(cd "${INSTALLER_DIR}/.." && pwd)

VERSION=${1:?"Usage: $0 <version> <linux-amd64|linux-arm64> [output_dir]"}
TARGET_PLATFORM=${2:?"Usage: $0 <version> <linux-amd64|linux-arm64> [output_dir]"}
OUTPUT_DIR=${3:-"${REPO_DIR}/offline-packages"}

case "${TARGET_PLATFORM}" in
  amd64 | x86_64 | linux-amd64)
    TARGET_PLATFORM="linux-amd64"
    TARGET_ARCH="amd64"
    ;;
  arm64 | aarch64 | linux-arm64)
    TARGET_PLATFORM="linux-arm64"
    TARGET_ARCH="arm64"
    ;;
  *)
    echo "Unsupported target platform: ${TARGET_PLATFORM}" >&2
    echo "Use linux-amd64 or linux-arm64." >&2
    exit 1
    ;;
esac

BACKEND_IMAGE=${BACKEND_IMAGE:-"ghcr.io/sevoniva/crest-service:${VERSION}"}
FRONTEND_IMAGE=${FRONTEND_IMAGE:-"ghcr.io/sevoniva/crest-web:${VERSION}"}
SOURCE_BACKEND_IMAGE=${SOURCE_BACKEND_IMAGE:-"${BACKEND_IMAGE}"}
SOURCE_FRONTEND_IMAGE=${SOURCE_FRONTEND_IMAGE:-"${FRONTEND_IMAGE}"}
PACKAGE_NAME="crest-app-images-${VERSION}-${TARGET_PLATFORM}"
PACKAGE_DIR="${OUTPUT_DIR}/${PACKAGE_NAME}"
UPGRADE_SQL_FROM_1_5_7="v1.5.7_to_${VERSION}.sql"
if [ ! -f "${INSTALLER_DIR}/upgrade-sql/${UPGRADE_SQL_FROM_1_5_7}" ]; then
  UPGRADE_SQL_FROM_1_5_7="v1.5.7_to_v1.5.8.sql"
fi
UPGRADE_SQL_TARGET_VERSION="${UPGRADE_SQL_FROM_1_5_7#v1.5.7_to_}"
UPGRADE_SQL_TARGET_VERSION="${UPGRADE_SQL_TARGET_VERSION%.sql}"

log() {
  echo "[app-image-package] $*"
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

safe_image_file_name() {
  echo "$1" | sed 's#/#_#g; s#:#_#g'
}

image_arch() {
  docker image inspect --format '{{.Architecture}}' "$1" 2>/dev/null | head -n 1
}

require_cmd docker
require_cmd tar

if ! docker image inspect "${SOURCE_BACKEND_IMAGE}" >/dev/null 2>&1; then
  echo "Source backend image not found: ${SOURCE_BACKEND_IMAGE}" >&2
  echo "Build, pull, or tag the backend image first, or set SOURCE_BACKEND_IMAGE." >&2
  exit 1
fi

if ! docker image inspect "${SOURCE_FRONTEND_IMAGE}" >/dev/null 2>&1; then
  echo "Source frontend image not found: ${SOURCE_FRONTEND_IMAGE}" >&2
  echo "Build, pull, or tag the frontend image first, or set SOURCE_FRONTEND_IMAGE." >&2
  exit 1
fi

if [ "$(image_arch "${SOURCE_BACKEND_IMAGE}")" != "${TARGET_ARCH}" ]; then
  echo "Source backend image architecture mismatch: ${SOURCE_BACKEND_IMAGE} is $(image_arch "${SOURCE_BACKEND_IMAGE}"), expected ${TARGET_ARCH}." >&2
  exit 1
fi

if [ "$(image_arch "${SOURCE_FRONTEND_IMAGE}")" != "${TARGET_ARCH}" ]; then
  echo "Source frontend image architecture mismatch: ${SOURCE_FRONTEND_IMAGE} is $(image_arch "${SOURCE_FRONTEND_IMAGE}"), expected ${TARGET_ARCH}." >&2
  exit 1
fi

rm -rf "${PACKAGE_DIR}"
mkdir -p "${PACKAGE_DIR}/images"

if [ -d "${INSTALLER_DIR}/upgrade-sql" ]; then
  log "Copy upgrade SQL"
  cp -R "${INSTALLER_DIR}/upgrade-sql" "${PACKAGE_DIR}/upgrade-sql"
fi

log "Copy metadata initialization SQL"
mkdir -p "${PACKAGE_DIR}/init-sql"
cp -R "${REPO_DIR}/core/core-backend/src/main/resources/db/migration" "${PACKAGE_DIR}/init-sql/mysql"
cp -R "${REPO_DIR}/core/core-backend/src/main/resources/db/migration-ob-mysql" "${PACKAGE_DIR}/init-sql/ob-mysql"
cp -R "${REPO_DIR}/core/core-backend/src/main/resources/db/migration-ob-oracle" "${PACKAGE_DIR}/init-sql/ob-oracle"

log "Tag backend image ${SOURCE_BACKEND_IMAGE} -> ${BACKEND_IMAGE}"
docker tag "${SOURCE_BACKEND_IMAGE}" "${BACKEND_IMAGE}"

log "Tag frontend image ${SOURCE_FRONTEND_IMAGE} -> ${FRONTEND_IMAGE}"
docker tag "${SOURCE_FRONTEND_IMAGE}" "${FRONTEND_IMAGE}"

BACKEND_ARCHIVE="${PACKAGE_DIR}/images/$(safe_image_file_name "${BACKEND_IMAGE}").tar.gz"
FRONTEND_ARCHIVE="${PACKAGE_DIR}/images/$(safe_image_file_name "${FRONTEND_IMAGE}").tar.gz"

log "Save ${BACKEND_IMAGE}"
docker save "${BACKEND_IMAGE}" | gzip -c > "${BACKEND_ARCHIVE}"

log "Save ${FRONTEND_IMAGE}"
docker save "${FRONTEND_IMAGE}" | gzip -c > "${FRONTEND_ARCHIVE}"

cat > "${PACKAGE_DIR}/load-images.sh" <<'EOF'
#!/bin/bash
set -euo pipefail

if ! command -v docker >/dev/null 2>&1; then
  echo "Missing required command: docker" >&2
  exit 1
fi

for archive in images/*.tar.gz; do
  [ -e "${archive}" ] || {
    echo "No image archive found in images/." >&2
    exit 1
  }
  echo "Loading ${archive}"
  docker load -i "${archive}"
done
EOF
chmod +x "${PACKAGE_DIR}/load-images.sh"

cat > "${PACKAGE_DIR}/image-manifest.txt" <<EOF
version=${VERSION}
platform=${TARGET_PLATFORM}
backend_image=${BACKEND_IMAGE}
frontend_image=${FRONTEND_IMAGE}
generated_at=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
EOF

cat > "${PACKAGE_DIR}/APP-IMAGES-README.md" <<EOF
# Crest 应用镜像升级包 ${VERSION} (${TARGET_PLATFORM})

本包仅包含 Crest 后端镜像、前端镜像、增量升级 SQL 和加载说明，不包含 MySQL、Prometheus、Grafana 镜像，也不包含完整安装脚本。适用于已经安装 Crest 的内网环境升级应用服务。

## 包内内容

- \`images/\`：Crest 后端和前端镜像归档；
- \`init-sql/\`：空库部署时可由 DBA 手工执行的元数据库初始化 SQL；
- \`upgrade-sql/\`：从已发布版本升级所需的增量 SQL；
- \`load-images.sh\`：加载包内镜像；
- \`image-manifest.txt\`：包内镜像与目标架构。

## 加载镜像

\`\`\`bash
tar -zxf ${PACKAGE_NAME}.tar.gz
cd ${PACKAGE_NAME}
bash load-images.sh
\`\`\`

## 执行升级 SQL

全新安装不需要执行升级 SQL。已有 v1.5.7 环境升级到 ${VERSION} 时，请先备份元数据库，再在本包目录执行 ${UPGRADE_SQL_TARGET_VERSION} 元数据库升级 SQL：

\`\`\`bash
set -a
source /opt/crest/.env
set +a
docker exec -i mysql-crest mysql -uroot -p"\${CREST_DB_PASSWORD}" "\${CREST_DB_NAME:-crest}" < upgrade-sql/${UPGRADE_SQL_FROM_1_5_7}
\`\`\`

已有 v1.5.4 环境升级到 ${VERSION} 时，请先执行 v1.5.4 到 v1.5.5 的升级 SQL，再执行 v1.5.7 到 ${UPGRADE_SQL_TARGET_VERSION} 的升级 SQL：

\`\`\`bash
set -a
source /opt/crest/.env
set +a
docker exec -i mysql-crest mysql -uroot -p"\${CREST_DB_PASSWORD}" "\${CREST_DB_NAME:-crest}" < upgrade-sql/v1.5.4_to_v1.5.5.sql
docker exec -i mysql-crest mysql -uroot -p"\${CREST_DB_PASSWORD}" "\${CREST_DB_NAME:-crest}" < upgrade-sql/${UPGRADE_SQL_FROM_1_5_7}
\`\`\`

外部 MySQL 环境可在数据库服务器或运维机执行同一 SQL 文件，目标库为 Crest 元数据库。
如安装时修改过 \`CREST_BASE\`，请把 \`/opt/crest/.env\` 换成实际运行目录中的 \`.env\`。

OceanBase MySQL 元数据库可使用 OceanBase 客户端执行 MySQL 兼容升级 SQL：

\`\`\`bash
obclient -h<obproxy-host> -P2883 -u'<user>@<tenant>#<cluster>' -p'<password>' <database> < upgrade-sql/${UPGRADE_SQL_FROM_1_5_7}
\`\`\`

OceanBase Oracle 元数据库使用独立 Flyway migration 路径。除非发布说明明确提供 OB Oracle 专用升级 SQL，不要把 MySQL 兼容升级 SQL 执行到 OB Oracle 租户。

## OceanBase 空库初始化

OceanBase 元数据库按企业外部数据库处理。生产环境建议先由 DBA 创建租户、数据库或 schema、账号和权限，然后按文件名顺序执行 \`init-sql/ob-mysql/\` 或 \`init-sql/ob-oracle/\` 下的初始化 SQL。Crest 运行配置中保持 \`CREST_FLYWAY_ENABLED=false\`，服务启动时只连接已初始化的外部元数据库。

## 更新运行配置

默认运行目录为 \`/opt/crest\`。如安装时修改过 \`CREST_BASE\`，请进入实际运行目录。

\`\`\`bash
cd /opt/crest
cp .env ".env.bak.\$(date +%Y%m%d%H%M%S)"

if grep -q '^CREST_BACKEND_IMAGE=' .env; then
  sed -i.bak 's#^CREST_BACKEND_IMAGE=.*#CREST_BACKEND_IMAGE=${BACKEND_IMAGE}#' .env
else
  printf '\\nCREST_BACKEND_IMAGE=${BACKEND_IMAGE}\\n' >> .env
fi

if grep -q '^CREST_FRONTEND_IMAGE=' .env; then
  sed -i.bak 's#^CREST_FRONTEND_IMAGE=.*#CREST_FRONTEND_IMAGE=${FRONTEND_IMAGE}#' .env
else
  printf 'CREST_FRONTEND_IMAGE=${FRONTEND_IMAGE}\\n' >> .env
fi

docker-compose up -d --no-deps crest-service crest
\`\`\`

## 验证

\`\`\`bash
cd /opt/crest
docker-compose ps
docker logs crest-service --tail 200
docker logs crest --tail 200
\`\`\`
EOF

log "Create archive"
find "${PACKAGE_DIR}" \( -name '._*' -o -name '.DS_Store' \) -delete
COPYFILE_DISABLE=1 tar -zcf "${OUTPUT_DIR}/${PACKAGE_NAME}.tar.gz" -C "${OUTPUT_DIR}" "${PACKAGE_NAME}"

log "Package ready: ${OUTPUT_DIR}/${PACKAGE_NAME}.tar.gz"
