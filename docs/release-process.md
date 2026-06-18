# Crest 分支、补丁和发布管理规范

本文定义 Crest 的长期分支、临时分支、补丁版本和发布包规则。所有开发、发布和自动化流程均以本文为准。

## 核心原则

- 已发布 tag 不移动、不删除、不重打。`v1.5.5` 一旦发布，业务修复必须进入后续补丁版本，例如 `v1.5.6`。
- 发布包内容异常但代码和镜像未变时，可以重跑对应打包 workflow 覆盖同名附件；业务代码、SQL、配置或前端产物变化时必须发新版本。
- 补丁版本只做最小修复，不混入新功能、重构或无关格式化。
- 数据库变更必须提供增量 SQL，并进入完整离线包和应用镜像升级包。
- 所有受保护分支和 tag 必须通过 Pull Request、Release Guard 和 GitHub Rulesets；管理员不绕过规则。

## 长期分支

| 分支 | 用途 | 合入方式 |
| --- | --- | --- |
| `main` | 稳定主线，代表最新可发布状态 | 只接受 PR，不直接 push |
| `dev` | 新功能和非紧急修复集成分支 | 只接受 PR，不直接 push |
| `release/<major.minor>` | 已发布稳定线维护分支，例如 `release/1.5`、`release/2.0` | 只接受 PR，不直接 push |

## 临时分支命名

| 场景 | 分支格式 | 目标分支 |
| --- | --- | --- |
| 新功能 | `feat/<short-name>` | `dev` |
| 普通缺陷，尚未发布 | `fix/<short-name>` | `dev` |
| 已发布版本补丁 | `hotfix/<next-version>-<short-name>` | 对应 `release/<major.minor>` |
| 安全修复 | `security/<short-name>` 或 `hotfix/<next-version>-security-<short-name>` | `dev` 或对应 `release/<major.minor>` |
| 发布流程、CI、打包脚本 | `ci/<short-name>` 或 `chore/<short-name>` | `dev`、`main` 或对应 `release/<major.minor>`，按影响范围选择 |
| 文档 | `docs/<short-name>` | 对应长期分支 |
| 实验验证 | `spike/<short-name>` | 不直接合入受保护分支，需整理为正式分支后再 PR |

## 常见场景

### 1. 已发布版本发现业务 bug

示例：已交付的 `v1.5.5` 环境有 bug，需要重新打包交付。

1. 从维护分支创建补丁分支：

   ```bash
   git checkout release/1.5
   git pull --ff-only
   git checkout -b hotfix/1.5.6-short-description
   ```

2. 做最小修复和回归测试。
3. 将版本从 `1.5.5` 提升到 `1.5.6`。
4. 更新 `CHANGELOG.md`，新增 `docs/release/v1.5.6.md`。
5. 如涉及数据库、系统参数、初始化数据或结构变化，新增 `installer/upgrade-sql/v1.5.5_to_v1.5.6.sql`。
6. PR 合入 `release/1.5`。
7. 从 `release/1.5` 打 `v1.5.6` tag，触发镜像、完整离线包、应用镜像升级包和源码包。
8. 将补丁同步回 `main`，必要时同步回 `dev`。

不要覆盖 `v1.5.5` 的 tag 或业务包。

### 2. 发布附件或打包流程异常

代码、镜像和 SQL 没变，只是 Release 附件漏传、checksum 异常、说明文档未刷新时：

- 修复 workflow 或打包脚本；
- 合入 `main`；
- 重新运行对应 workflow；
- 仅覆盖同一 tag 的附件。

如果任何业务代码、前端产物、后端镜像内容或 SQL 改变，必须发新补丁版本。

### 3. 新功能

新功能从 `feat/*` 分支进入 `dev`。功能稳定后，按版本计划从 `dev` 合入 `main` 或未来的 release 分支。新功能不得直接进入已发布稳定线的 release 分支，除非明确决定作为补丁版本的一部分并完成风险评审。

### 4. 普通缺陷

尚未发布到稳定版本的缺陷，从 `fix/*` 进入 `dev`。如果同一问题已经影响发布版本，再从对应 `release/<major.minor>` 创建 `hotfix/*` 做最小补丁，不直接 cherry-pick 大范围开发分支。

### 5. 安全问题

安全问题按影响范围选择：

- 只影响未发布开发线：`security/*` -> `dev`；
- 影响已发布版本：`hotfix/<next-version>-security-<short-name>` -> 对应 `release/<major.minor>`；
- 修复后同步回 `main` 和 `dev`。

### 6. SQL 变更

已经发布的初始化脚本不改写。补丁升级使用 `installer/upgrade-sql/` 下的增量 SQL。

命名规则：

```text
installer/upgrade-sql/v<from>_to_v<to>.sql
```

示例：

```text
installer/upgrade-sql/v1.5.5_to_v1.5.6.sql
```

从 `v1.5.4` 升级到 `v1.5.6` 时，文档必须说明按顺序执行：

```text
v1.5.4_to_v1.5.5.sql
v1.5.5_to_v1.5.6.sql
```

## 发布检查

发布稳定版本或补丁版本前必须满足：

- `VERSION`、Maven 根版本、`crest.version`、前端 `package.json` 版本一致；
- 新版本号大于仓库中最新稳定 tag；
- `CHANGELOG.md` 有新版本章节；
- `docs/release/vX.Y.Z.md` 存在；
- 如有数据库变更，存在对应增量 SQL；
- Release Guard 通过；
- 镜像构建 workflow 通过；
- 完整离线包、应用镜像升级包、源码包和 checksum 都上传到 Release。

## 协作要求

- 需求不明确时，先判定是否影响已发布版本、数据库结构、部署脚本或发布附件；
- 涉及已发布版本的问题，优先按 hotfix 流程处理；
- 涉及新功能或规划能力的问题，从功能分支进入集成分支；
- 涉及 SQL、系统参数、初始化数据或表结构的问题，必须同步维护 `installer/upgrade-sql/`、离线包内容和发布说明；
- 不移动、不删除、不覆盖已发布 tag，除非已按事故流程完成风险确认。
