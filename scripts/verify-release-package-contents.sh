#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "release-package-check: $*" >&2
  exit 1
}

usage() {
  echo "Usage: $0 <offline|app-images> <version> <linux-amd64|linux-arm64> [package_file]" >&2
}

if [[ $# -lt 3 || $# -gt 4 ]]; then
  usage
  exit 64
fi

package_type="$1"
version="$2"
platform="$3"
package_file="${4:-}"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

case "${platform}" in
  linux-amd64)
    arch="amd64"
    ;;
  linux-arm64)
    arch="arm64"
    ;;
  *)
    fail "Unsupported platform ${platform}."
    ;;
esac

case "${package_type}" in
  offline)
    package_dir="crest-offline-${version}-${platform}"
    package_file="${package_file:-offline-packages/${package_dir}.tar.gz}"
    ;;
  app-images)
    package_dir="crest-app-images-${version}-${platform}"
    package_file="${package_file:-offline-packages/${package_dir}.tar.gz}"
    ;;
  *)
    fail "Unsupported package type ${package_type}."
    ;;
esac

[[ -f "${package_file}" ]] || fail "Package file not found: ${package_file}"

package_list="$(mktemp)"
trap 'rm -f "${package_list}"' EXIT
tar -tzf "${package_file}" > "${package_list}"

require_entry() {
  local entry="$1"
  grep -Fqx "${package_dir}/${entry}" "${package_list}" || fail "Missing ${entry} in ${package_file}."
}

reject_entry() {
  local pattern="$1"
  if grep -Eq "${pattern}" "${package_list}"; then
    fail "Package contains unwanted file matching ${pattern}."
  fi
}

require_tree_files() {
  local source_dir="$1"
  local package_prefix="$2"

  [[ -d "${source_dir}" ]] || fail "Source directory not found: ${source_dir}"

  while IFS= read -r -d '' file; do
    local base_name="${file##*/}"
    local relative_path="${file#${source_dir}/}"

    case "${base_name}" in
      .DS_Store | ._*) continue ;;
    esac

    require_entry "${package_prefix}/${relative_path}"
  done < <(find "${source_dir}" -type f -print0)
}

require_tree_files "${repo_root}/installer/upgrade-sql" "upgrade-sql"
require_tree_files "${repo_root}/core/core-backend/src/main/resources/db/migration" "init-sql/mysql"
require_tree_files "${repo_root}/core/core-backend/src/main/resources/db/migration-ob-mysql" "init-sql/ob-mysql"
require_tree_files "${repo_root}/core/core-backend/src/main/resources/db/migration-ob-oracle" "init-sql/ob-oracle"

case "${package_type}" in
  offline)
    require_entry "LICENSE"
    require_entry "README.md"
    require_entry "OFFLINE-README.md"
    require_entry "install.conf"
    require_entry "install.sh"
    require_entry "uninstall.sh"
    require_entry "crestctl"
    require_entry "crest/docker-compose.yml"
    require_entry "images/ghcr.io_sevoniva_crest-service_${version}-${arch}.tar.gz"
    require_entry "images/ghcr.io_sevoniva_crest-web_${version}-${arch}.tar.gz"
    require_entry "images/mysql_8.0.43.tar.gz"

    if [[ "${EXPECT_OBSERVABILITY_IMAGES:-false}" == "true" ]]; then
      require_entry "images/prom_prometheus_v3.12.0.tar.gz"
      require_entry "images/grafana_grafana-oss_13.0.1.tar.gz"
    fi
    ;;
  app-images)
    require_entry "APP-IMAGES-README.md"
    require_entry "load-images.sh"
    require_entry "image-manifest.txt"
    require_entry "images/ghcr.io_sevoniva_crest-service_${version}.tar.gz"
    require_entry "images/ghcr.io_sevoniva_crest-web_${version}.tar.gz"
    ;;
esac

reject_entry '(^|/)\._'
reject_entry '(^|/)\.DS_Store$'

echo "release-package-check: ${package_file} passed."
