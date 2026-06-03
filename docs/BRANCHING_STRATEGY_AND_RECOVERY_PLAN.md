# Crest 分支管理规范 & 修复恢复计划

## 一、当前问题诊断

### 现状
| 对象 | 状态 |
|------|------|
| `v1.5.4` (tag) | **最后一个稳定版本**，测试通过 |
| `main` | 在 `v1.5.4` 之后有 **8 个提交**（全是安全/代码质量修复），**测试未通过** |
| `fix/v1.5.4-dataset` | 基于 `v1.5.4`，有 1 个业务修复（数据表拖拽），独立在外 |
| 其他 `feat/*` / `security/*` 分支 | 基线混乱，部分基于 v1.3.0/v1.5.0 等旧版本 |

### 根本原因
- `main` 被直接推送了未经验证的提交，失去了"随时可发布"的基线。
- 没有受保护的中间层（release 分支），安全修复和业务修复混在一起，无法独立验证。

---

## 二、分支管理规范（建议采用）

```
main ──────────────────────────────► 永远可发布，受保护
  │                                    只能由 release/* 合并进入
  │
  ├── release/v1.5.5 ──────────────► 从 v1.5.4 切出，逐步修复验证
  │       │                            验证通过后合并回 main，打 tag
  │       ├── fix/security-scan-1    从 release 切出，单点修复
  │       ├── fix/ssrf-cleanup       从 release 切出，单点修复
  │       └── fix/dataset-drag       从 release 切出，业务修复
  │
  ├── feat/lineage-knowledge-graph   从 main 切出，长期功能
  ├── feat/admin-management          从 main 切出，长期功能
  └── security/fix-vulnerabilities   从 release/main 切出，安全修复
```

### 核心规则

| 分支类型 | 创建基线 | 命名规则 | 合并目标 | 生命周期 |
|---------|---------|---------|---------|---------|
| `main` | — | `main` | 不可直接 push | 永久 |
| `release/v{major}.{minor}.{patch}` | 上一个 release tag | `release/v1.5.5` | `main` | 到下一个 release 结束 |
| `fix/{scope}-{desc}` | `release/*` 或 `main` | `fix/security-ssrf` | `release/*` → `main` | 合并后删除 |
| `feat/{scope}-{desc}` | `main` 或最新 `release` | `feat/lineage-graph` | `main`（通过 PR） | 合并后删除 |
| `security/{date}-{desc}` | `release/*` | `security/20260603-ssrf` | `release/*` → `main` | 合并后删除 |
| `hotfix/{desc}` | `main`（紧急线上修复） | `hotfix/critical-xss` | `main` + `release/*` | 合并后删除 |

### 铁律
1. **禁止直接 push 到 `main`**，所有变更必须通过 PR/MR 合并。
2. **`release/*` 分支必须通过全部测试后才能合并到 `main`**。
3. **每个 `fix/*` 分支只解决一个问题**，原子性提交，方便回滚。
4. **打 tag 只能在 `main` 上进行**，tag 即 release。

---

## 三、修复恢复计划（从 v1.5.4 → v1.5.5）

### 提交分组（main 上 v1.5.4 之后的 8 个提交）

main 上的提交按依赖/领域分为 **4 个阶段**：

| 阶段 | 提交 | 主题 | 风险 | 建议分支名 |
|-----|------|------|------|-----------|
| **P1 基础安全** | `0f93ca459` | Harden security scan findings | 🔴 大（35 文件）| `fix/v1.5.5-security-scan` |
| | `7a3a575e8` | Harden frontend sanitization | 🟡 中（2 文件）| （同上或独立） |
| | `1f2eb9e84` | Improve exception logging | 🟢 低（27 文件，纯日志）| （同上或独立） |
| **P2 安全质量** | `77c5cbccc` | Harden security quality findings | 🔴 大（20 文件，含加密/部署）| `fix/v1.5.5-security-quality` |
| **P3 扫描修复** | `2997a7771` | Resolve code scanning alerts | 🟡 中（3 文件）| `fix/v1.5.5-code-scan` |
| | `df8c8575c` | Annotate false positives | 🟢 低（2 文件，注释）| （同上） |
| **P4 架构清理** | `79cd38af4` | Use metadata APIs | 🔴 大（CalciteProvider 重构）| `fix/v1.5.5-metadata-api` |
| | `6199a45f7` | Remove remote download SSRF | 🟡 中（删除 HttpClientUtil）| `fix/v1.5.5-remove-ssrf` |

> 另外 `fix/v1.5.4-dataset` 的业务修复（`80191b538`）也要纳入，建议在 P1 之前 cherry-pick，因为它最稳定。

### 执行步骤

```bash
# Step 0: 确保在 main，且最新
git checkout main && git pull origin main

# Step 1: 创建 release 基线分支（从 v1.5.4）
git checkout -b release/v1.5.5 v1.5.4

# Step 2: 先合并最稳定的业务修复（fix/v1.5.4-dataset）
git cherry-pick 80191b538
# → 跑测试，通过后继续

# Step 3: P1 基础安全修复（可拆成 1~3 个 fix 分支）
# 方案 A：逐个 cherry-pick（推荐，方便定位测试失败点）
git cherry-pick 0f93ca459   # security scan
git cherry-pick 7a3a575e8   # frontend sanitization
git cherry-pick 1f2eb9e84   # exception logging
# → 每步跑测试

# 方案 B：如果 P1 内部有依赖，可合为一个 fix 分支
# git checkout -b fix/v1.5.5-p1-security-base release/v1.5.5
# （在分支上 cherry-pick，验证后合并回 release/v1.5.5）

# Step 4: P2 安全质量
git cherry-pick 77c5cbccc
# → 跑测试（此提交涉及 AesUtils/RsaUtils/FileUtils，测试必须过）

# Step 5: P3 代码扫描修复
git cherry-pick 2997a7771
git cherry-pick df8c8575c
# → 跑测试

# Step 6: P4 架构清理（CalciteProvider 重构 + SSRF 移除）
git cherry-pick 79cd38af4
git cherry-pick 6199a45f7
# → 跑测试

# Step 7: release/v1.5.5 全部通过测试后
# - 打 tag: git tag v1.5.5
# - 合并到 main（通过 PR 或直接 merge --ff-only 如果线性历史）
# - push tag + main

# Step 8: 清理 fix/v1.5.4-dataset（已合并，可删除）
git branch -d fix/v1.5.4-dataset
git push origin --delete fix/v1.5.4-dataset
```

### 如果 cherry-pick 冲突怎么办？

main 上的 8 个提交是顺序提交的，理论上互相之间不会有冲突，但 `fix/v1.5.4-dataset`（`80191b538`）和 `0f93ca459` 可能修改了同一个前端文件。如果冲突：

1. **在单独的 `fix/*` 分支上解决冲突**，不要直接在 `release/*` 上硬修。
2. 冲突解决后，在该 `fix/*` 分支跑通测试，再合并回 `release/*`。

### main 的最终处理

`main` 当前有 8 个未通过测试的提交。推荐做法：

```bash
# 方案 A：硬重置（推荐，如果 main 只有你自己在推）
git checkout main
git reset --hard release/v1.5.5   # main 和 release 完全一致
git push origin main --force-with-lease

# 方案 B：merge --ff-only（如果 main 之后没有新的合法提交）
git checkout main
git merge --ff-only release/v1.5.5
git push origin main
```

> ⚠️ **提醒**：如果 `main` 已经被其他人拉取过，force push 会影响他人。请确认当前 `main` 只有你一个人在用，再选择方案 A。

---

## 四、清理建议（现有混乱分支）

| 分支 | 建议操作 | 原因 |
|------|---------|------|
| `origin/fix/v1.5.4-dataset` | ✅ 合并后删除 | 业务修复，已纳入 release/v1.5.5 |
| `origin/tmp-v2.10.22` | ❓ 确认是否还需要 | 临时分支，看起来是初始化快照 |
| `origin/feat/simplify-internal` | ❓ 确认功能是否已废弃 | 基线在 v1.5.0 之前，可能已过时 |
| `origin/feat/lineage-knowledge-graph` | ❓ 确认进度 | 基线在 v1.2.0，可能需要 rebase 到 main |
| `origin/feat/security-hardening-2.10.23` | ❓ 确认是否已合并 | 看起来是历史分支，可能已经合入 v1.3.0/v1.5.x |
| `origin/security/fix-vulnerabilities-20260529` | ❓ 确认内容 | 基线在很早期，可能内容已过时 |

**建议**：release/v1.5.5 稳定后，让各 feature 负责人 rebase 到最新的 `main`（v1.5.5），过期的分支直接删除。

---

## 五、下一步（需要你确认）

1. **确认分支规范**：上面的 `release/fix/feat/security` 模型是否符合你的工作流？
2. **确认 main 重置方式**：你当前是单人开发还是多人协作？`main` 能否 force push？
3. **确认修复范围**：main 上 8 个安全提交 + dataset 修复，是否全部要纳入 v1.5.5？
4. **确认测试命令**：你的测试是怎么跑的？`mvn test`？前端 `pnpm test`？我需要知道才能帮你在每步 cherry-pick 后自动验证。

如果你确认以上几点，我可以立即帮你：
- 创建 `release/v1.5.5`
- 逐步 cherry-pick + 跑测试
- 生成最终合并的 main
