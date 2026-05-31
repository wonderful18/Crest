#!/usr/bin/env bash
set -euo pipefail

STRICT=false
if [[ "${1:-}" == "--strict" ]]; then
  STRICT=true
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

EXCLUDES=(
  --glob '!target/**'
  --glob '!offline-packages/**'
  --glob '!.crest-local/**'
  --glob '!.crest-verify-*/**'
  --glob '!node_modules/**'
  --glob '!dist/**'
  --glob '!core/core-backend/src/main/resources/static/**'
  --glob '!core/core-frontend/dist/**'
  --glob '!scripts/governance/scan-service-governance.sh'
)

run_scan() {
  local title="$1"
  local pattern="$2"
  shift 2

  echo
  echo "## ${title}"
  if rg -n "${pattern}" "${EXCLUDES[@]}" "$@" .; then
    return 1
  fi
  echo "No matches"
  return 0
}

failures=0

run_scan "Legacy public environment variables" "\\bDE_[A-Z0-9_]+\\b" \
  --glob '!docs/governance/service-governance-design.md' \
  --glob '!docs/superpowers/plans/2026-05-31-service-governance.md' || failures=$((failures + 1))

run_scan "Legacy Crest runtime identifiers" "\\b(DeApiPath|DeLog|DeLogAspect|DEException|DeModel|DataEaseBi|DeResourceTree|DeRichText|DePreview|DE-DV-CATCH|dataease-embedded|de_inner_params)\\b" \
  --glob '!docs/governance/service-governance-design.md' \
  --glob '!docs/superpowers/plans/2026-05-31-service-governance.md' || failures=$((failures + 1))

run_scan "Legacy database names" "\\b(dataease_name|de_type|de_extract_type|de_sync_dataset_|de_standalone_version|de_template_version|core_de_engine|core_chart_view|core_chart_view_snapshot|snapshot_core_chart_view|core_dataset_group|core_dataset_table|core_dataset_table_field|core_opt_recent|data_visualization_info|snapshot_data_visualization_info|visualization_info|visualization_snapshot_info|per_busi_resource|per_auth_busi_user|per_auth_busi_role|per_user_role|per_role)\\b" \
  --glob '!docs/governance/service-governance-design.md' \
  --glob '!docs/superpowers/plans/2026-05-31-service-governance.md' || failures=$((failures + 1))

run_scan "Legacy platform table references" '(@TableName\("(core_|crest_user|crest_org|crest_role|crest_resource|sso_|data_visualization_|snapshot_data_visualization_|per_|de_)|CREATE TABLE `?(core_|crest_user|crest_org|crest_role|crest_resource|sso_|data_visualization_|snapshot_data_visualization_|per_|de_|area\b)|ALTER TABLE `?(core_|crest_user|crest_org|crest_role|crest_resource|sso_|data_visualization_|snapshot_data_visualization_|per_|de_|area\b)|INSERT INTO `?(core_|crest_user|crest_org|crest_role|crest_resource|sso_|data_visualization_|snapshot_data_visualization_|per_|de_|area\b)|FROM `?(core_|crest_user|crest_org|crest_role|crest_resource|sso_|data_visualization_|snapshot_data_visualization_|per_|de_|area\b)|JOIN `?(core_|crest_user|crest_org|crest_role|crest_resource|sso_|data_visualization_|snapshot_data_visualization_|per_|de_|area\b)|UPDATE `?(core_|crest_user|crest_org|crest_role|crest_resource|sso_|data_visualization_|snapshot_data_visualization_|per_|de_|area\b)|DELETE FROM `?(core_|crest_user|crest_org|crest_role|crest_resource|sso_|data_visualization_|snapshot_data_visualization_|per_|de_|area\b))' \
  --glob 'sdk/**/*.java' \
  --glob 'core/core-backend/src/main/java/**/*.java' \
  --glob 'core/core-backend/src/main/resources/**/*.xml' \
  --glob 'core/core-backend/src/main/resources/**/*.sql' \
  --glob 'core/core-backend/src/main/resources/**/*.yml' || failures=$((failures + 1))

run_scan "Legacy action-style API paths" "@(Get|Post|Put|Delete|Patch)Mapping\\(\\\"/?(find|query|get|save|update|delete|del|batchDel|create|edit|start|stop|execute|pager)(-|/|\\\"|[A-Z])" \
  --glob 'sdk/api/**/*.java' \
  --glob 'core/core-backend/src/main/java/**/*.java' || failures=$((failures + 1))

run_scan "Legacy action-style API path segments" "@(Get|Post|Put|Delete|Patch)Mapping\\(\\\"[^\\\"]*(record-new|batch-removal|all-removal|logical-removal|field-removal|chart-field-removal|table-removal|removalAll|chartRemoval|removeJumpSet|removeLinkage|canvas-change|base-change|/add|/remove|/del|/change|batch/del)(/|\\\")" \
  --glob 'sdk/api/**/*.java' \
  --glob 'core/core-backend/src/main/java/**/*.java' || failures=$((failures + 1))

run_scan "Legacy frontend action-style API paths" "url:\\s*[\`'\\\"][^\`'\\\"]*(record-new|batch-removal|all-removal|logical-removal|field-removal|chart-field-removal|table-removal|removalAll|chartRemoval|removeJumpSet|removeLinkage|canvas-change|base-change|/add|/remove|/del|/change|batch/del)(/|[\`'\\\"])" \
  --glob 'core/core-frontend/src/**/*.ts' \
  --glob 'core/core-frontend/src/**/*.vue' || failures=$((failures + 1))

if [[ "$STRICT" == "true" && "$failures" -gt 0 ]]; then
  echo
  echo "Service governance strict scan failed with ${failures} finding group(s)."
  exit 1
fi

echo
echo "Service governance scan completed with ${failures} finding group(s)."
if [[ "$STRICT" == "false" ]]; then
  echo "Run with --strict after the migration to enforce zero findings."
fi
