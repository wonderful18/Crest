#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: scripts/prepare-release.sh <version>"
  echo
  echo "Examples:"
  echo "  scripts/prepare-release.sh 1.5.5"
  echo "  scripts/prepare-release.sh v1.5.5"
}

if [[ $# -ne 1 ]]; then
  usage >&2
  exit 64
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${repo_root}"

target_version="${1#v}"
if [[ ! "${target_version}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Version must use x.y.z format." >&2
  exit 64
fi

target_tag="v${target_version}"
current_version="$(tr -d '[:space:]' < VERSION)"
current_version="${current_version#v}"
current_tag="v${current_version}"

if [[ "${target_version}" == "${current_version}" ]]; then
  echo "Already on ${target_tag}."
  exit 0
fi

replace_literal() {
  local file="$1"
  local old_value="$2"
  local new_value="$3"

  perl -0pi -e 'BEGIN { ($old, $new) = @ARGV; splice @ARGV, 0, 2 } s/\Q$old\E/$new/g' \
    "${old_value}" "${new_value}" "${file}"
}

replace_tag_in_current_docs() {
  local file="$1"

  perl -pi -e 'BEGIN { ($old, $new) = @ARGV; splice @ARGV, 0, 2 } if ($_ !~ /docs\/release\/v/) { s/\Q$old\E/$new/g }' \
    "${current_tag}" "${target_tag}" "${file}"
}

replace_package_version() {
  local file="$1"

  perl -0pi -e 'BEGIN { ($old, $new) = @ARGV; splice @ARGV, 0, 2 } s/("version"\s*:\s*)"\Q$old\E"/$1"$new"/g' \
    "${current_version}" "${target_version}" "${file}"
}

update_kubernetes_image_tags() {
  local files=(
    "deploy/kubernetes/08-crest-service-deployment.yaml"
    "deploy/kubernetes/11-crest-web-deployment.yaml"
  )

  perl -0pi -e 'BEGIN { $tag = shift @ARGV } s#(ghcr\.io/sevoniva/crest-service:)v[0-9]+\.[0-9]+\.[0-9]+#$1$tag#g; s#(ghcr\.io/sevoniva/crest-web:)v[0-9]+\.[0-9]+\.[0-9]+#$1$tag#g' \
    "${target_tag}" "${files[@]}"
}

update_workflow_release_defaults() {
  local files=(
    ".github/workflows/docker-publish.yml"
    ".github/workflows/release-app-image-packages.yml"
    ".github/workflows/release-offline-packages.yml"
  )

  perl -0pi -e 'BEGIN { $tag = shift @ARGV } s/(default:\s*)v[0-9]+\.[0-9]+\.[0-9]+/${1}${tag}/g; s/(example\s+)v[0-9]+\.[0-9]+\.[0-9]+/${1}${tag}/g; s/(github\.ref_name \|\| \x27)v[0-9]+\.[0-9]+\.[0-9]+(\x27)/${1}${tag}${2}/g' \
    "${target_tag}" "${files[@]}"
}

pom_files=(
  "pom.xml"
  "sdk/pom.xml"
  "sdk/common/pom.xml"
  "sdk/api/pom.xml"
  "sdk/api/api-base/pom.xml"
  "sdk/api/api-permissions/pom.xml"
  "sdk/api/api-sync/pom.xml"
  "sdk/extensions/pom.xml"
  "sdk/extensions/extensions-datasource/pom.xml"
  "sdk/extensions/extensions-view/pom.xml"
  "sdk/extensions/extensions-datafilling/pom.xml"
  "core/pom.xml"
  "core/core-backend/pom.xml"
  "core/core-frontend/pom.xml"
)

tag_files=(
  ".github/ISSUE_TEMPLATE/bug---.md"
  ".github/workflows/docker-publish.yml"
  ".github/workflows/release-offline-packages.yml"
  "installer/install.conf"
  "installer/crest/docker-compose.yml"
  "deploy/kubernetes/08-crest-service-deployment.yaml"
  "deploy/kubernetes/11-crest-web-deployment.yaml"
)

doc_files=(
  "README.md"
  "installer/README.md"
  "deploy/kubernetes/README.md"
)

printf '%s\n' "${target_version}" > VERSION

for file in "${pom_files[@]}"; do
  replace_literal "${file}" "${current_version}" "${target_version}"
done

replace_package_version "core/core-frontend/package.json"

for file in "${tag_files[@]}"; do
  replace_literal "${file}" "${current_tag}" "${target_tag}"
done
update_kubernetes_image_tags
update_workflow_release_defaults

for file in "${doc_files[@]}"; do
  replace_tag_in_current_docs "${file}"
done

echo "Prepared ${target_tag} from ${current_tag}."
echo "Next: add docs/release/${target_tag}.md and update CHANGELOG.md."
