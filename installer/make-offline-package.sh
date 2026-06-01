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
MYSQL_IMAGE=${MYSQL_IMAGE:-"mysql:8.0.43"}
INCLUDE_OBSERVABILITY_IMAGES=${INCLUDE_OBSERVABILITY_IMAGES:-false}
PROMETHEUS_IMAGE=${PROMETHEUS_IMAGE:-"prom/prometheus:v3.12.0"}
GRAFANA_IMAGE=${GRAFANA_IMAGE:-"grafana/grafana-oss:13.0.1"}
SOURCE_MYSQL_IMAGE=${SOURCE_MYSQL_IMAGE:-"${MYSQL_IMAGE}"}
SOURCE_PROMETHEUS_IMAGE=${SOURCE_PROMETHEUS_IMAGE:-"${PROMETHEUS_IMAGE}"}
SOURCE_GRAFANA_IMAGE=${SOURCE_GRAFANA_IMAGE:-"${GRAFANA_IMAGE}"}

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

BACKEND_IMAGE=${BACKEND_IMAGE:-"ghcr.io/sevoniva/crest-service:${VERSION}-${TARGET_ARCH}"}
FRONTEND_IMAGE=${FRONTEND_IMAGE:-"ghcr.io/sevoniva/crest-web:${VERSION}-${TARGET_ARCH}"}
SOURCE_BACKEND_IMAGE=${SOURCE_BACKEND_IMAGE:-"${BACKEND_IMAGE}"}
SOURCE_FRONTEND_IMAGE=${SOURCE_FRONTEND_IMAGE:-"${FRONTEND_IMAGE}"}
PACKAGE_NAME="crest-offline-${VERSION}-${TARGET_PLATFORM}"
PACKAGE_DIR="${OUTPUT_DIR}/${PACKAGE_NAME}"

log() {
  echo "[offline-package] $*"
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
  echo "Build or tag the backend image first, or set SOURCE_BACKEND_IMAGE." >&2
  exit 1
fi

if ! docker image inspect "${SOURCE_FRONTEND_IMAGE}" >/dev/null 2>&1; then
  echo "Source frontend image not found: ${SOURCE_FRONTEND_IMAGE}" >&2
  echo "Build or tag the frontend image first, or set SOURCE_FRONTEND_IMAGE." >&2
  exit 1
fi

if ! docker image inspect "${SOURCE_MYSQL_IMAGE}" >/dev/null 2>&1; then
  echo "Source MySQL image not found: ${SOURCE_MYSQL_IMAGE}" >&2
  echo "Pull or load it first, or set SOURCE_MYSQL_IMAGE." >&2
  exit 1
fi

if [ "${INCLUDE_OBSERVABILITY_IMAGES}" = "true" ]; then
  if ! docker image inspect "${SOURCE_PROMETHEUS_IMAGE}" >/dev/null 2>&1; then
    echo "Source Prometheus image not found: ${SOURCE_PROMETHEUS_IMAGE}" >&2
    echo "Pull or load it first, or set SOURCE_PROMETHEUS_IMAGE." >&2
    exit 1
  fi
  if ! docker image inspect "${SOURCE_GRAFANA_IMAGE}" >/dev/null 2>&1; then
    echo "Source Grafana image not found: ${SOURCE_GRAFANA_IMAGE}" >&2
    echo "Pull or load it first, or set SOURCE_GRAFANA_IMAGE." >&2
    exit 1
  fi
fi

if [ "$(image_arch "${SOURCE_BACKEND_IMAGE}")" != "${TARGET_ARCH}" ]; then
  echo "Source backend image architecture mismatch: ${SOURCE_BACKEND_IMAGE} is $(image_arch "${SOURCE_BACKEND_IMAGE}"), expected ${TARGET_ARCH}." >&2
  exit 1
fi

if [ "$(image_arch "${SOURCE_FRONTEND_IMAGE}")" != "${TARGET_ARCH}" ]; then
  echo "Source frontend image architecture mismatch: ${SOURCE_FRONTEND_IMAGE} is $(image_arch "${SOURCE_FRONTEND_IMAGE}"), expected ${TARGET_ARCH}." >&2
  exit 1
fi

if [ "$(image_arch "${SOURCE_MYSQL_IMAGE}")" != "${TARGET_ARCH}" ]; then
  echo "Source MySQL image architecture mismatch: ${SOURCE_MYSQL_IMAGE} is $(image_arch "${SOURCE_MYSQL_IMAGE}"), expected ${TARGET_ARCH}." >&2
  exit 1
fi

if [ "${INCLUDE_OBSERVABILITY_IMAGES}" = "true" ] && [ "$(image_arch "${SOURCE_PROMETHEUS_IMAGE}")" != "${TARGET_ARCH}" ]; then
  echo "Source Prometheus image architecture mismatch: ${SOURCE_PROMETHEUS_IMAGE} is $(image_arch "${SOURCE_PROMETHEUS_IMAGE}"), expected ${TARGET_ARCH}." >&2
  exit 1
fi

if [ "${INCLUDE_OBSERVABILITY_IMAGES}" = "true" ] && [ "$(image_arch "${SOURCE_GRAFANA_IMAGE}")" != "${TARGET_ARCH}" ]; then
  echo "Source Grafana image architecture mismatch: ${SOURCE_GRAFANA_IMAGE} is $(image_arch "${SOURCE_GRAFANA_IMAGE}"), expected ${TARGET_ARCH}." >&2
  exit 1
fi

rm -rf "${PACKAGE_DIR}"
mkdir -p "${PACKAGE_DIR}/images"

log "Copy installer files"
cp "${INSTALLER_DIR}/LICENSE" "${PACKAGE_DIR}/"
cp "${INSTALLER_DIR}/README.md" "${PACKAGE_DIR}/"
cp "${INSTALLER_DIR}/install.conf" "${PACKAGE_DIR}/"
cp "${INSTALLER_DIR}/install.sh" "${PACKAGE_DIR}/"
cp "${INSTALLER_DIR}/cleanup-demo-data.sh" "${PACKAGE_DIR}/"
cp "${INSTALLER_DIR}/uninstall.sh" "${PACKAGE_DIR}/"
cp "${INSTALLER_DIR}/crestctl" "${PACKAGE_DIR}/"
cp -R "${INSTALLER_DIR}/crest" "${PACKAGE_DIR}/crest"
mkdir -p "${PACKAGE_DIR}/images"

log "Pin install.conf image names"
sed -i.bak \
  -e "s#^CREST_BACKEND_IMAGE=.*#CREST_BACKEND_IMAGE=${BACKEND_IMAGE}#g" \
  -e "s#^CREST_FRONTEND_IMAGE=.*#CREST_FRONTEND_IMAGE=${FRONTEND_IMAGE}#g" \
  -e "s#^CREST_MYSQL_IMAGE=.*#CREST_MYSQL_IMAGE=${MYSQL_IMAGE}#g" \
  -e "s#^CREST_PROMETHEUS_IMAGE=.*#CREST_PROMETHEUS_IMAGE=${PROMETHEUS_IMAGE}#g" \
  -e "s#^CREST_GRAFANA_IMAGE=.*#CREST_GRAFANA_IMAGE=${GRAFANA_IMAGE}#g" \
  "${PACKAGE_DIR}/install.conf"
rm -f "${PACKAGE_DIR}/install.conf.bak"

log "Tag backend image ${SOURCE_BACKEND_IMAGE} -> ${BACKEND_IMAGE}"
docker tag "${SOURCE_BACKEND_IMAGE}" "${BACKEND_IMAGE}"

log "Tag frontend image ${SOURCE_FRONTEND_IMAGE} -> ${FRONTEND_IMAGE}"
docker tag "${SOURCE_FRONTEND_IMAGE}" "${FRONTEND_IMAGE}"

log "Tag MySQL image ${SOURCE_MYSQL_IMAGE} -> ${MYSQL_IMAGE}"
docker tag "${SOURCE_MYSQL_IMAGE}" "${MYSQL_IMAGE}"

if [ "${INCLUDE_OBSERVABILITY_IMAGES}" = "true" ]; then
  log "Tag Prometheus image ${SOURCE_PROMETHEUS_IMAGE} -> ${PROMETHEUS_IMAGE}"
  docker tag "${SOURCE_PROMETHEUS_IMAGE}" "${PROMETHEUS_IMAGE}"

  log "Tag Grafana image ${SOURCE_GRAFANA_IMAGE} -> ${GRAFANA_IMAGE}"
  docker tag "${SOURCE_GRAFANA_IMAGE}" "${GRAFANA_IMAGE}"
fi

BACKEND_ARCHIVE="${PACKAGE_DIR}/images/$(safe_image_file_name "${BACKEND_IMAGE}").tar.gz"
FRONTEND_ARCHIVE="${PACKAGE_DIR}/images/$(safe_image_file_name "${FRONTEND_IMAGE}").tar.gz"
MYSQL_ARCHIVE="${PACKAGE_DIR}/images/$(safe_image_file_name "${MYSQL_IMAGE}").tar.gz"
PROMETHEUS_ARCHIVE="${PACKAGE_DIR}/images/$(safe_image_file_name "${PROMETHEUS_IMAGE}").tar.gz"
GRAFANA_ARCHIVE="${PACKAGE_DIR}/images/$(safe_image_file_name "${GRAFANA_IMAGE}").tar.gz"

log "Save ${BACKEND_IMAGE}"
docker save "${BACKEND_IMAGE}" | gzip -c > "${BACKEND_ARCHIVE}"

log "Save ${FRONTEND_IMAGE}"
docker save "${FRONTEND_IMAGE}" | gzip -c > "${FRONTEND_ARCHIVE}"

log "Save ${MYSQL_IMAGE}"
docker save "${MYSQL_IMAGE}" | gzip -c > "${MYSQL_ARCHIVE}"

if [ "${INCLUDE_OBSERVABILITY_IMAGES}" = "true" ]; then
  log "Save ${PROMETHEUS_IMAGE}"
  docker save "${PROMETHEUS_IMAGE}" | gzip -c > "${PROMETHEUS_ARCHIVE}"

  log "Save ${GRAFANA_IMAGE}"
  docker save "${GRAFANA_IMAGE}" | gzip -c > "${GRAFANA_ARCHIVE}"
fi

cat > "${PACKAGE_DIR}/OFFLINE-README.md" <<EOF
# Crest 离线部署包 ${VERSION} (${TARGET_PLATFORM})

本目录包含 Crest 应用镜像、MySQL 镜像、Docker Compose 配置和安装脚本，可在无公网网络的服务器上直接安装。

## 安装

\`\`\`bash
tar -zxf ${PACKAGE_NAME}.tar.gz
cd ${PACKAGE_NAME}
bash install.sh
\`\`\`

安装前可按需调整 \`install.conf\`：

- \`CREST_PORT\`：Web 访问端口，默认 8100
- \`CREST_BASE\`：安装基目录，实际运行目录为 \`\${CREST_BASE}/crest\`
- \`CREST_BACKEND_IMAGE\`：Crest 后端镜像，已固定为本包内镜像
- \`CREST_FRONTEND_IMAGE\`：Crest 前端镜像，已固定为本包内镜像
- \`CREST_MYSQL_IMAGE\`：MySQL 镜像，已固定为本包内镜像
- \`CREST_INITIAL_PASSWORD\`：管理员初始密码；留空时安装脚本会随机生成并在安装完成后输出
- \`CREST_LOAD_DEMO\`：是否初始化内置演示资源，默认 \`true\`；生产环境不需要示例数据时设为 \`false\`
- \`CREST_PROMETHEUS_ENABLED\`：是否启用 Prometheus 指标采集，默认 \`false\`
- \`CREST_GRAFANA_ENABLED\`：是否启用 Grafana 看板，默认 \`false\`

已加载演示资源的环境可执行：

\`\`\`bash
bash cleanup-demo-data.sh --yes
\`\`\`

## 镜像

- 后端镜像：\`${BACKEND_IMAGE}\`
- 前端镜像：\`${FRONTEND_IMAGE}\`
- MySQL 镜像：\`${MYSQL_IMAGE}\`
- Prometheus 镜像：\`${PROMETHEUS_IMAGE}\`
- Grafana 镜像：\`${GRAFANA_IMAGE}\`
- 目标架构：\`${TARGET_PLATFORM}\`

安装脚本会自动执行 \`docker load\` 加载 \`images/\` 下的镜像文件。

## 默认账号

- 用户名：\`admin\`
- 密码：安装完成后终端输出的初始密码

请在首次登录后修改默认密码。
EOF

log "Create archive"
find "${PACKAGE_DIR}" \( -name '._*' -o -name '.DS_Store' \) -delete
COPYFILE_DISABLE=1 tar -zcf "${OUTPUT_DIR}/${PACKAGE_NAME}.tar.gz" -C "${OUTPUT_DIR}" "${PACKAGE_NAME}"

log "Package ready: ${OUTPUT_DIR}/${PACKAGE_NAME}.tar.gz"
