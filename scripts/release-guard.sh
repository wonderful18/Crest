#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "release-guard: $*" >&2
  exit 1
}

info() {
  echo "release-guard: $*"
}

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${repo_root}"

current_version="$(tr -d '[:space:]' < VERSION)"
[[ "${current_version}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "VERSION must use x.y.z format."

root_pom_version="$(sed -n 's/.*<version>\([^<]*\)<\/version>.*/\1/p' pom.xml | head -n 1)"
crest_property_version="$(sed -n 's/.*<crest.version>\([^<]*\)<\/crest.version>.*/\1/p' pom.xml | head -n 1)"
frontend_version="$(sed -n 's/^[[:space:]]*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' core/core-frontend/package.json | head -n 1)"

[[ "${root_pom_version}" == "${current_version}" ]] || fail "pom.xml version ${root_pom_version} does not match VERSION ${current_version}."
[[ "${crest_property_version}" == "${current_version}" ]] || fail "pom.xml crest.version ${crest_property_version} does not match VERSION ${current_version}."
[[ "${frontend_version}" == "${current_version}" ]] || fail "frontend package version ${frontend_version} does not match VERSION ${current_version}."

[[ -f "docs/release/v${current_version}.md" ]] || fail "Missing docs/release/v${current_version}.md."
grep -Eq "^## v${current_version}[[:space:]-]" CHANGELOG.md || fail "CHANGELOG.md is missing v${current_version} section."

target_branch="${GITHUB_BASE_REF:-${GITHUB_REF_NAME:-}}"
source_branch="${GITHUB_HEAD_REF:-${GITHUB_REF_NAME:-}}"
main_sync_ref="refs/tmp/release-guard-main"

if [[ -n "${GITHUB_BASE_REF:-}" ]]; then
  info "pull request ${source_branch} -> ${target_branch}"
  git fetch --no-tags --prune origin "${GITHUB_BASE_REF}:${GITHUB_BASE_REF}" >/dev/null 2>&1 || true
  git fetch --no-tags origin "refs/heads/main:${main_sync_ref}" >/dev/null 2>&1 || true
elif [[ -n "${GITHUB_REF_NAME:-}" ]]; then
  info "push/workflow branch ${GITHUB_REF_NAME}"
fi

case "${target_branch}" in
  main)
    case "${source_branch}" in
      dev | release/* | hotfix/* | fix/* | chore/* | ci/* | docs/*) ;;
      feat/*) fail "Feature branches must target dev first, not main." ;;
      main) ;;
      *) fail "PRs to main must come from dev, release/*, hotfix/*, fix/*, chore/*, ci/*, or docs/*." ;;
    esac
    ;;
  dev)
    case "${source_branch}" in
      feat/* | fix/* | chore/* | ci/* | docs/* | refactor/* | test/* | security/* | hotfix/* | dev) ;;
      *) fail "PRs to dev must use feat/*, fix/*, chore/*, ci/*, docs/*, refactor/*, test/*, security/*, or hotfix/*." ;;
    esac
    ;;
  release/*)
    case "${source_branch}" in
      hotfix/* | fix/* | chore/* | ci/* | docs/* | release/*) ;;
      feat/*) fail "Feature branches must not target release branches." ;;
      *) fail "PRs to release branches must use hotfix/*, fix/*, chore/*, ci/*, docs/*, or release/*." ;;
    esac
    ;;
esac

changed_files=""
if [[ -n "${GITHUB_BASE_REF:-}" ]]; then
  if git rev-parse --verify "${GITHUB_BASE_REF}" >/dev/null 2>&1; then
    changed_files="$(git diff --name-only "${GITHUB_BASE_REF}...HEAD")"
  else
    changed_files="$(git diff --name-only "origin/${GITHUB_BASE_REF}...HEAD")"
  fi
elif [[ -n "${EVENT_BEFORE:-}" && ! "${EVENT_BEFORE}" =~ ^0+$ ]]; then
  changed_files="$(git diff --name-only "${EVENT_BEFORE}...HEAD" || true)"
fi

latest_tag="$(git tag -l 'v[0-9]*.[0-9]*.[0-9]*' --sort=-version:refname | head -n 1 || true)"
latest_version="${latest_tag#v}"

semver_gt() {
  local left="$1"
  local right="$2"
  local la lb lc ra rb rc
  IFS=. read -r la lb lc <<< "${left}"
  IFS=. read -r ra rb rc <<< "${right}"
  if (( la != ra )); then (( la > ra )); return; fi
  if (( lb != rb )); then (( lb > rb )); return; fi
  (( lc > rc ))
}

tag_exists() {
  git rev-parse -q --verify "refs/tags/$1" >/dev/null
}

matches_tag_content() {
  local tag="$1"
  local file="$2"
  git cat-file -e "${tag}:${file}" 2>/dev/null && git diff --quiet "${tag}..HEAD" -- "${file}"
}

matches_ref_content() {
  local ref="$1"
  local file="$2"
  git cat-file -e "${ref}:${file}" 2>/dev/null && git diff --quiet "${ref}..HEAD" -- "${file}"
}

allows_dev_sync_from_main() {
  local file="$1"
  [[ "${target_branch}" == "dev" ]] && matches_ref_content "${main_sync_ref}" "${file}"
}

requires_release_metadata=0
db_change=0

while IFS= read -r file; do
  [[ -n "${file}" ]] || continue

  if [[ "${file}" =~ ^docs/release/v([0-9]+\.[0-9]+\.[0-9]+)\.md$ ]]; then
    tagged_version="v${BASH_REMATCH[1]}"
    if tag_exists "${tagged_version}" && ! matches_tag_content "${tagged_version}" "${file}" && ! allows_dev_sync_from_main "${file}"; then
      fail "Published release notes ${file} are immutable because ${tagged_version} already exists."
    fi
  fi

  if [[ "${file}" =~ ^installer/upgrade-sql/v[0-9]+\.[0-9]+\.[0-9]+_to_v([0-9]+\.[0-9]+\.[0-9]+)\.sql$ ]]; then
    tagged_version="v${BASH_REMATCH[1]}"
    if tag_exists "${tagged_version}" && ! matches_tag_content "${tagged_version}" "${file}" && ! allows_dev_sync_from_main "${file}"; then
      fail "Published upgrade SQL ${file} is immutable because ${tagged_version} already exists."
    fi
  fi

  case "${file}" in
    VERSION | pom.xml | core/core-frontend/package.json | docs/release/v*.md | CHANGELOG.md)
      requires_release_metadata=1
      ;;
    core/* | sdk/* | drivers/* | staticResource/*)
      [[ "${target_branch}" == release/* ]] && requires_release_metadata=1
      ;;
    installer/*)
      case "${file}" in
        installer/README.md | installer/upgrade-sql/*) ;;
        *) [[ "${target_branch}" == release/* ]] && requires_release_metadata=1 ;;
      esac
      ;;
  esac

  case "${file}" in
    core/core-backend/src/main/resources/db/* | db/* | *.sql)
      db_change=1
      ;;
  esac
done <<< "${changed_files}"

if [[ "${target_branch}" == release/* && "${requires_release_metadata}" == "1" ]]; then
  [[ -n "${latest_tag}" ]] || fail "No existing stable tag found for patch comparison."
  semver_gt "${current_version}" "${latest_version}" || fail "Release branch changes must bump VERSION above latest tag ${latest_tag}; current is v${current_version}."
  [[ -f "docs/release/v${current_version}.md" ]] || fail "Missing release notes for v${current_version}."
  grep -Eq "^## v${current_version}[[:space:]-]" CHANGELOG.md || fail "Missing CHANGELOG.md section for v${current_version}."

  if [[ "${db_change}" == "1" ]]; then
    expected_sql="installer/upgrade-sql/v${latest_version}_to_v${current_version}.sql"
    [[ -f "${expected_sql}" ]] || fail "Database changes on release branches require ${expected_sql}."
  fi
fi

info "passed for v${current_version}"
