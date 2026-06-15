# Comment Coverage And Private Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring front-end and back-end source comments to an auditable 100% declaration/contract coverage target, clean poor comments, and move public unit tests into a private repository integration path.

**Architecture:** Establish the audit script and policy first, then annotate high-risk modules in batches. Move tests through a private repository checkout flow so the public repository no longer contains test source while CI can still run private tests when credentials are present.

**Tech Stack:** Java 21, Maven, Spring Boot, Vue 3, TypeScript, Node built-in test runner, GitHub Actions, Git submodules or secondary checkout.

---

### Task 1: Baseline Audit And Policy

**Files:**
- Create: `scripts/comment-coverage-audit.mjs`
- Create: `docs/comment-coverage.md`
- Create: `docs/superpowers/plans/2026-06-15-comment-coverage-private-tests.md`

- [x] **Step 1: Add the comment audit script**

Create `scripts/comment-coverage-audit.mjs` with Git-tracked source discovery, comment/code statistics, declaration coverage detection, large-file prioritization, and banned comment pattern scanning.

- [x] **Step 2: Add the policy and migration planning document**

Create `docs/comment-coverage.md` with the coverage definition, forbidden comment patterns, phased source-code batches, and private test repository options.

- [x] **Step 3: Run the audit script**

Run:

```bash
node scripts/comment-coverage-audit.mjs
```

Expected: the command prints a baseline report with non-100% declaration coverage and a ranked list of files to annotate.

- [ ] **Step 4: Commit and push**

Run:

```bash
git add scripts/comment-coverage-audit.mjs docs/comment-coverage.md docs/superpowers/plans/2026-06-15-comment-coverage-private-tests.md
git commit -m "chore: 完善注释相关"
git push -u origin feat/v1.5.12-development
```

Expected: the branch exists on `origin` with the planning and audit baseline commit.

### Task 2: Data Source Comment Pass

**Files:**
- Modify: `core/core-backend/src/main/java/io/crest/datasource/server/DatasourceServer.java`
- Modify: `core/core-backend/src/main/java/io/crest/datasource/provider/CalciteProvider.java`
- Modify: `core/core-backend/src/main/java/io/crest/datasource/provider/ExcelUtils.java`
- Modify: `core/core-backend/src/main/java/io/crest/datasource/provider/ApiUtils.java`
- Modify: `core/core-backend/src/main/java/io/crest/datasource/security/SqlSecurityPolicy.java`

- [ ] **Step 1: Add class-level contract comments**

For each file, add a professional class-level comment that explains responsibility, boundaries, side effects, and what the class does not own.

- [ ] **Step 2: Add method-level comments to public and cross-module methods**

For every public, protected, and cross-module private method in these files, add a comment that explains parameters, returned shape, permissions, transaction boundary, and failure mode where relevant.

- [ ] **Step 3: Add block comments to complex data paths**

Add block comments before SQL dialect branching, read-only connection handling, Excel row ID management, API data parsing, driver loading, and permission checks.

- [ ] **Step 4: Remove poor or sensitive comments**

Run:

```bash
node scripts/comment-coverage-audit.mjs --json > /tmp/crest-comment-audit.json
```

Expected: the `bannedCommentIssues` array contains no hits from the modified files.

- [ ] **Step 5: Verify the batch**

Run:

```bash
node scripts/comment-coverage-audit.mjs
mvn -pl core/core-backend -DskipTests compile
```

Expected: audit output shows these files no longer appear in the top declaration-coverage gaps, and Maven compile exits 0.

- [ ] **Step 6: Commit and push**

Run:

```bash
git add core/core-backend/src/main/java/io/crest/datasource/server/DatasourceServer.java core/core-backend/src/main/java/io/crest/datasource/provider/CalciteProvider.java core/core-backend/src/main/java/io/crest/datasource/provider/ExcelUtils.java core/core-backend/src/main/java/io/crest/datasource/provider/ApiUtils.java core/core-backend/src/main/java/io/crest/datasource/security/SqlSecurityPolicy.java
git commit -m "chore: 完善注释相关"
git push
```

Expected: one pushed commit containing only comment changes in the data source batch.

### Task 3: Dataset And Synchronization Comment Pass

**Files:**
- Modify: `core/core-backend/src/main/java/io/crest/dataset/manage/DatasetDataManage.java`
- Modify: `core/core-backend/src/main/java/io/crest/dataset/manage/DatasetGroupManage.java`
- Modify: `core/core-backend/src/main/java/io/crest/dataset/manage/DatasetSQLManage.java`
- Modify: `core/core-backend/src/main/java/io/crest/dataset/sync/DatasetSyncManage.java`
- Modify: `core/core-backend/src/main/java/io/crest/dataset/sync/DatasetSyncTaskManage.java`
- Modify: `core/core-frontend/src/views/visualized/data/dataset/form/index.vue`
- Modify: `core/core-frontend/src/views/visualized/data/dataset/form/AddSql.vue`
- Modify: `core/core-frontend/src/views/visualized/data/dataset/form/DatasetUnion.vue`

- [ ] **Step 1: Document dataset structure and SQL assembly contracts**

Add comments for dataset tree shape, field mapping, union SQL construction, parameter replacement, permission validation, and sync trigger ownership.

- [ ] **Step 2: Document front-end dataset form state**

Add comments for form sections, async loaders, save flow, field selection, validation state, and user-visible side effects.

- [ ] **Step 3: Verify the batch**

Run:

```bash
node scripts/comment-coverage-audit.mjs
mvn -pl core/core-backend -DskipTests compile
cd core/core-frontend && pnpm run ts:check
```

Expected: audit output shows the modified files no longer appear as highest-priority comment gaps, Maven compile exits 0, and TypeScript check exits 0.

- [ ] **Step 4: Commit and push**

Run:

```bash
git add core/core-backend/src/main/java/io/crest/dataset/manage/DatasetDataManage.java core/core-backend/src/main/java/io/crest/dataset/manage/DatasetGroupManage.java core/core-backend/src/main/java/io/crest/dataset/manage/DatasetSQLManage.java core/core-backend/src/main/java/io/crest/dataset/sync/DatasetSyncManage.java core/core-backend/src/main/java/io/crest/dataset/sync/DatasetSyncTaskManage.java core/core-frontend/src/views/visualized/data/dataset/form/index.vue core/core-frontend/src/views/visualized/data/dataset/form/AddSql.vue core/core-frontend/src/views/visualized/data/dataset/form/DatasetUnion.vue
git commit -m "chore: 完善注释相关"
git push
```

Expected: one pushed commit containing only comment changes in the dataset batch.

### Task 4: Chart And Visualization Comment Pass

**Files:**
- Modify: `core/core-backend/src/main/java/io/crest/chart/utils/ChartDataBuild.java`
- Modify: `core/core-backend/src/main/java/io/crest/chart/manage/ChartDataManage.java`
- Modify: `core/core-backend/src/main/java/io/crest/chart/server/ChartDataServer.java`
- Modify: `core/core-backend/src/main/java/io/crest/visualization/server/DataVisualizationServer.java`
- Modify: `core/core-frontend/src/views/chart/components/editor/index.vue`
- Modify: `core/core-frontend/src/custom-component/v-query/QueryConditionConfiguration.vue`
- Modify: `core/core-frontend/src/components/data-visualization/canvas/CanvasCore.vue`

- [ ] **Step 1: Document chart data transformations**

Add comments for AntV result shapes, pivot/merge behavior, auxiliary headers, dynamic labels/tooltips, drill-down, linkage, and export mode.

- [ ] **Step 2: Document visualization editor state flows**

Add comments for query component state, cascade logic, time and numeric range parameters, canvas state, snapshot compatibility, and publish/share behavior.

- [ ] **Step 3: Verify the batch**

Run:

```bash
node scripts/comment-coverage-audit.mjs
mvn -pl core/core-backend -DskipTests compile
cd core/core-frontend && pnpm run ts:check
```

Expected: audit output shows the modified files no longer appear as highest-priority comment gaps, Maven compile exits 0, and TypeScript check exits 0.

- [ ] **Step 4: Commit and push**

Run:

```bash
git add core/core-backend/src/main/java/io/crest/chart/utils/ChartDataBuild.java core/core-backend/src/main/java/io/crest/chart/manage/ChartDataManage.java core/core-backend/src/main/java/io/crest/chart/server/ChartDataServer.java core/core-backend/src/main/java/io/crest/visualization/server/DataVisualizationServer.java core/core-frontend/src/views/chart/components/editor/index.vue core/core-frontend/src/custom-component/v-query/QueryConditionConfiguration.vue core/core-frontend/src/components/data-visualization/canvas/CanvasCore.vue
git commit -m "chore: 完善注释相关"
git push
```

Expected: one pushed commit containing only comment changes in the chart and visualization batch.

### Task 5: SDK, API, Entity, And Remaining Source Sweep

**Files:**
- Modify: files reported by `node scripts/comment-coverage-audit.mjs --json` in `lowDeclarationCoverageFiles`
- Modify: `sdk/**/*.java`
- Modify: `core/core-backend/src/main/java/io/crest/**/dao/auto/entity/*.java`
- Modify: remaining `core/core-frontend/src/**/*.{vue,ts,js,mjs,css,less}`

- [ ] **Step 1: Generate the remaining gap list**

Run:

```bash
node scripts/comment-coverage-audit.mjs --json > /tmp/crest-comment-audit.json
node -e "const r=require('/tmp/crest-comment-audit.json'); for (const f of r.lowDeclarationCoverageFiles) console.log(f.file)"
```

Expected: the command prints the exact remaining files that still need declaration comments.

- [ ] **Step 2: Annotate remaining SDK and API files**

For each file printed from `sdk` or API packages, add comments for public extension points, request/response shapes, serialization rules, compatibility behavior, and failure modes.

- [ ] **Step 3: Annotate automatic entities without repeating accessors**

For each automatic entity, add class-level and field-level comments. Do not add one-line comments to every getter/setter unless the accessor changes observable behavior.

- [ ] **Step 4: Annotate remaining front-end declarations**

For each remaining front-end file, add comments for component responsibility, state groups, public functions, watchers, computed values, asynchronous side effects, and cross-component contracts.

- [ ] **Step 5: Verify 100% declaration coverage**

Run:

```bash
node scripts/comment-coverage-audit.mjs --strict
```

Expected: the command exits 0 with no banned comment issues and 100% declaration coverage.

- [ ] **Step 6: Commit and push**

Run:

```bash
git add core sdk
git commit -m "chore: 完善注释相关"
git push
```

Expected: one or more pushed commits containing only comment changes from the remaining source sweep.

### Task 6: Private Test Repository Migration

**Files:**
- Modify: `.gitignore`
- Create: `scripts/sync-private-tests.sh`
- Create: `.github/workflows/private-tests.yml`
- Remove from public repository: `core/core-backend/src/test/java/**`
- Remove from public repository: `core/core-frontend/src/**/*.test.mjs`
- Create in private repository: `core-backend/src/test/java/**`
- Create in private repository: `core-frontend/src/**/*.test.mjs`

- [ ] **Step 1: Create the ignored private test mount path**

Add `.private-tests/` to `.gitignore`.

- [ ] **Step 2: Create the sync script**

Create `scripts/sync-private-tests.sh` that accepts the private checkout path, removes stale public test copies, and syncs private tests into the Maven and front-end source locations for the current working tree.

- [ ] **Step 3: Add the private test workflow**

Create `.github/workflows/private-tests.yml` with a normal public checkout, a second private checkout into `.private-tests`, the sync script, Maven tests, and front-end Node tests.

- [ ] **Step 4: Move tests to the private repository**

Move all current tracked test files from the public repository into the private repository paths listed above. Preserve paths under the private root so the sync script can copy them back into the expected build locations.

- [ ] **Step 5: Remove public test files from Git tracking**

Run:

```bash
git rm -r core/core-backend/src/test/java
git ls-files 'core/core-frontend/src/**/*.test.mjs' | xargs git rm
```

Expected: public Git tracking removes test sources while the private repository keeps them.

- [ ] **Step 6: Verify public and private behavior**

Run without private tests:

```bash
mvn -pl core/core-backend -DskipTests compile
cd core/core-frontend && pnpm run ts:check
```

Run with private tests after checkout:

```bash
bash scripts/sync-private-tests.sh .private-tests
mvn -pl core/core-backend test
find core/core-frontend/src -name '*.test.mjs' -print0 | xargs -0 node --test
```

Expected: public compile works without test sources, and private checkout runs the full test suite.

- [ ] **Step 7: Commit and push**

Run:

```bash
git add .gitignore scripts/sync-private-tests.sh .github/workflows/private-tests.yml
git add -u core
git commit -m "chore: 完善注释相关"
git push
```

Expected: the public branch no longer contains unit test source files and contains only the private test integration hooks.
