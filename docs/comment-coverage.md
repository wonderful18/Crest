# 注释覆盖和私有测试规划

本文固定 1.5.12 开发线的注释改造口径、审计方式和私有测试仓库迁移方案。目标是提升代码阅读和维护效率，不通过机械注释堆数字。

## 当前基线

- 后端主代码、SDK、前端源码都存在注释不足；最大风险集中在数据源、数据集、图表、可视化编辑器和过滤组件。
- 公开仓库当前跟踪了后端 `core/core-backend/src/test/java` 下的 JUnit 测试，以及前端 `core/core-frontend/src/**/*.test.mjs` 下的 Node 测试。
- 当前 GitHub 工作流主要是发布守卫和打包流程，`release-guard` 不执行这些单元测试。
- 仓库没有 Checkstyle、PMD、Sonar、Typedoc 或 Javadoc 覆盖门禁，注释覆盖必须先由本仓库脚本定义并持续审计。

## 注释覆盖口径

“100% 覆盖”按注释点计算，不按代码行计算。纳入范围内的每个注释点都必须有专业、稳定、可维护的注释。

必须覆盖的注释点：

- Java 的类、接口、枚举、record、公开方法、保护方法、跨模块调用的私有方法。
- Vue、TypeScript、JavaScript 中的组件职责、props/emits 契约、store 状态、computed/watch、副作用函数、异步流程和跨组件 provide/inject。
- SQL 拼接、数据转换、权限过滤、兼容历史数据、缓存同步、连接池、驱动加载、导入导出和安全校验。
- 大文件中的阶段性逻辑块，尤其是输入形状、输出形状、排序/过滤规则、错误处理策略和兼容原因。
- 测试迁移前保留在公开仓库中的测试入口和测试辅助脚本。

禁止写入或必须清理的注释：

- 只复述代码动作的注释，例如“设置变量”“循环列表”“返回结果”。
- 带有个人名、公司名、作者署名、聊天机器人痕迹或生成痕迹的注释。
- 失效、误导、与代码行为不一致的历史注释。
- 无法验证的承诺式注释，例如“后续会优化”“临时处理”。
- 大段解释实现细节但没有说明业务约束的注释。

低价值但仍需处理的代码：

- 自动实体、简单 DTO、常量类和 getter/setter 不逐个写重复注释；改为补类级用途、字段级业务含义和生成来源说明。
- 本地化字典、第三方静态资源、构建产物、SVG、字体、图片不纳入代码注释覆盖。
- 已迁出到私有仓库的测试源码不再要求公开仓库注释覆盖，但私有仓库内部仍按相同规则维护。

## 审计脚本

运行：

```bash
node scripts/comment-coverage-audit.mjs
```

输出内容：

- 被审计源码总数、代码行、注释行、注释/代码比例。
- Java、Vue、TypeScript、JavaScript、样式文件的声明级注释覆盖率。
- 按业务域分组的注释密度。
- 大文件低注释密度列表。
- 声明缺少邻近注释的文件和示例行。
- 注释中疑似作者署名、生成痕迹或聊天机器人痕迹的命中项。

严格模式：

```bash
node scripts/comment-coverage-audit.mjs --strict
```

严格模式在发现禁止注释痕迹，或声明级注释覆盖未达到 100% 时返回非零退出码。初期用于人工观察；等首批改造完成后再接入 CI。

私有敏感词扫描：

```bash
COMMENT_AUDIT_BANLIST_FILE=/absolute/path/to/private-banlist.txt node scripts/comment-coverage-audit.mjs
```

私有 banlist 每行一个正则表达式。公司名、个人名和内部代号不要提交到公开仓库。

## 注释改造批次

### 第一批：数据源和 SQL 路径

范围：

- `core/core-backend/src/main/java/io/crest/datasource/server/DatasourceServer.java`
- `core/core-backend/src/main/java/io/crest/datasource/provider/CalciteProvider.java`
- `core/core-backend/src/main/java/io/crest/datasource/provider/ExcelUtils.java`
- `core/core-backend/src/main/java/io/crest/datasource/provider/ApiUtils.java`
- `core/core-backend/src/main/java/io/crest/datasource/security/SqlSecurityPolicy.java`

验收：

- 每个类有职责和边界说明。
- 每个公开入口有参数含义、权限/事务/副作用说明。
- SQL 拼接、方言转换、只读连接、Excel 编辑、远程文件加载和 API 数据源解析都有约束说明。
- 审计脚本中这些文件的声明级注释覆盖达到 100%。

### 第二批：数据集和同步

范围：

- `core/core-backend/src/main/java/io/crest/dataset/manage`
- `core/core-backend/src/main/java/io/crest/dataset/sync`
- `core/core-frontend/src/views/visualized/data/dataset`

验收：

- 数据集树、字段映射、联合数据集、自定义 SQL、同步任务状态和缓存状态有业务规则说明。
- 前端大表单的状态变量、异步加载、保存流程和校验流程有分块注释。
- 审计脚本不再把相关大文件列入最高优先级低注释密度列表。

### 第三批：图表和可视化编辑

范围：

- `core/core-backend/src/main/java/io/crest/chart`
- `core/core-backend/src/main/java/io/crest/visualization`
- `core/core-frontend/src/views/chart`
- `core/core-frontend/src/components/data-visualization`

验收：

- 图表数据转换、同环比、下钻、联动、阈值、导出和快照兼容逻辑有注释。
- 前端编辑器、画布、样式面板和图表渲染配置有注释。
- 与发布态、分享态、编辑态差异有关的注释准确说明读写边界。

### 第四批：SDK、API 契约和自动实体

范围：

- `sdk`
- `core/core-backend/src/main/java/io/crest/**/api`
- `core/core-backend/src/main/java/io/crest/**/dao/auto/entity`

验收：

- API、VO、DTO、插件扩展点有契约说明。
- 自动实体保留类级说明和字段级说明，不为每个 getter/setter 增加重复描述。
- 公开扩展点说明异常、权限、兼容和序列化约束。

## 私有测试仓库方案

### 推荐方案 A：私有测试仓库二次 checkout

适用目标：公开仓库不显示测试内容，也不把私有测试仓库作为 submodule 暴露给普通访客。

做法：

1. 新建私有仓库，例如 `sevoniva/Crest-private-tests`。
2. 把当前公开仓库的后端测试迁移到私有仓库的 `core-backend/src/test/java`。
3. 把当前公开仓库的前端 `.test.mjs` 迁移到私有仓库的 `core-frontend/src` 对应路径。
4. 公开仓库删除这些测试文件，并在 `.gitignore` 中保留 `.private-tests/`。
5. GitHub Actions 使用只读 deploy key、fine-grained PAT 或 GitHub App token checkout 私有仓库到 `.private-tests/`。
6. CI 在运行测试前把 `.private-tests/core-backend/src/test/java` 同步到 `core/core-backend/src/test/java`，把 `.private-tests/core-frontend/src` 中的 `.test.mjs` 同步到前端源码对应路径。
7. 本地开发者通过私有凭证运行同样的同步脚本；没有权限的人只能构建公开源码，不能获取测试内容。

工作流形态：

```yaml
- uses: actions/checkout@v6

- uses: actions/checkout@v6
  with:
    repository: sevoniva/Crest-private-tests
    path: .private-tests
    ssh-key: ${{ secrets.CREST_PRIVATE_TESTS_DEPLOY_KEY }}
    persist-credentials: false

- run: bash scripts/sync-private-tests.sh .private-tests
```

优点：

- 公开仓库不记录私有测试仓库的 commit 指针。
- 后续可以轮换凭证，不改公开 Git 历史。
- 访客不会看到测试源码。

代价：

- 需要新增同步脚本和 CI job。
- 没有私有仓库权限的 fork PR 不能运行完整单元测试。

### 方案 B：私有测试仓库作为 Git submodule

适用目标：接受公开仓库显示一个私有测试仓库路径和 commit 指针，只隐藏测试内容。

做法：

```bash
git submodule add git@github.com:sevoniva/Crest-private-tests.git private-tests
git commit -m "chore: 完善注释相关"
```

优点：

- 本地开发体验接近普通目录。
- Git 能精确记录测试仓库版本。

代价：

- `.gitmodules` 会公开私有仓库 URL。
- 公开仓库会记录 submodule commit SHA。
- 普通访客会看到一个无法拉取内容的目录指针。
- GitHub Actions 需要额外 SSH key、PAT 或 GitHub App token 才能拉取 submodule。

### 凭证选择

- 单个私有测试仓库：使用只读 deploy key，权限最小。
- 多个私有仓库或组织级管理：使用 GitHub App installation token。
- 个人维护且短期过渡：使用 fine-grained PAT，只授予目标私有仓库 Contents read 权限。

## 提交和推送规则

- 注释相关提交信息统一使用简短描述，例如 `chore: 完善注释相关`。
- 不在提交信息里逐项列文件或写实现细节。
- 每批改造完成后运行审计脚本和相关编译/测试命令，再提交并推送当前分支。
- 当前分支固定为 `feat/v1.5.12-development`。
