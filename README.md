# Crest

Crest 是面向私有化部署的开源 BI 平台，提供数据源接入、数据集建模、仪表盘、数据大屏、数据血缘和单点登录等能力。基于 DataEase 2.10.22 开源版本继续开发，合入 2.10.23 安全加固，按 GPLv3 发布。

**当前版本：v1.5.40**

## 功能概览

| 模块 | 说明 |
| --- | --- |
| 数据源 | MySQL、OceanBase Oracle，支持 Excel/CSV 导入 |
| 数据集 | 字段管理、计算字段、预览、结果缓存与增量同步 |
| 仪表盘 | 图表、筛选、联动、跳转、分享 |
| 数据大屏 | 面向展示墙和驾驶舱的可视化页面 |
| 数据血缘 | 数据源到展示资源的字段级依赖追踪 |
| 单点登录 | OIDC / OAuth2 授权码模式，支持 JWKS 签名验证 |
| 平台管理 | 用户、组织、角色、菜单与资源权限 |
| 可观测性 | 可选 Prometheus + Grafana 指标采集 |

## 快速安装

**单机 Docker 部署：**

```bash
cd installer
bash install.sh
```

安装完成后访问 `http://<host>:8100`，默认管理员账号 `admin`，初始密码由安装脚本随机生成。

**Kubernetes 部署：**

```bash
kubectl create namespace crest
kubectl -n crest create secret generic crest-db-secret \
  --from-literal=CREST_DB_USERNAME=root \
  --from-literal=CREST_DB_PASSWORD='<your-password>' \
  --from-literal=CREST_INITIAL_PASSWORD='<admin-password>'
kubectl apply -k deploy/kubernetes/internal-mysql
```

详见 [deploy/kubernetes/README.md](./deploy/kubernetes/README.md)。

## Docker 镜像

```text
ghcr.io/sevoniva/crest-service:v1.5.40   # 后端
ghcr.io/sevoniva/crest-web:v1.5.40        # 前端网关
```

## 本地开发

| 依赖 | 版本 |
| --- | --- |
| JDK | 21 |
| Maven | 3.9+ |
| Node.js | 22 |
| pnpm | 11 |
| MySQL | 8.0+ |

```bash
# 前端
cd core/core-frontend
pnpm install --frozen-lockfile
pnpm dev

# 后端
export CREST_DB_HOST=127.0.0.1
export CREST_DB_PORT=3306
export CREST_DB_NAME=crest
export CREST_DB_USERNAME=root
export CREST_DB_PASSWORD='<password>'
export CREST_AES_KEY='<32-char-key>'
export CREST_AES_IV='<16-char-iv>'
export CREST_INITIAL_PASSWORD='<admin-password>'
./run.sh start
```

详见 [docs/development.md](./docs/development.md)。

## 项目结构

```
core/core-backend    Spring Boot 后端服务
core/core-frontend   Vue 3 + Vite 前端工程
sdk/                 API 契约、公共模型、数据源扩展
installer/           单机安装脚本与离线包制作
deploy/              Kubernetes 部署清单
docs/                技术文档与发布说明
```

## 升级

从 v1.5.17 升级只需替换前后端镜像，无需执行升级 SQL。更早版本升级请参照 [CHANGELOG.md](./CHANGELOG.md) 中对应版本的安装说明。

## 文档

| 文档 | 内容 |
| --- | --- |
| [CHANGELOG.md](./CHANGELOG.md) | 版本变更记录 |
| [docs/release/v1.5.18.md](./docs/release/v1.5.18.md) | v1.5.18 发布说明 |
| [docs/development.md](./docs/development.md) | 开发说明、构建指南与提交规范 |
| [docs/data-lineage.md](./docs/data-lineage.md) | 数据血缘 |
| [docs/sso.md](./docs/sso.md) | 单点登录配置 |
| [docs/observability.md](./docs/observability.md) | 可观测性接入 |
| [installer/README.md](./installer/README.md) | 单机安装、离线包与升级 |
| [deploy/kubernetes/README.md](./deploy/kubernetes/README.md) | Kubernetes 部署 |
| [core/core-frontend/README.md](./core/core-frontend/README.md) | 前端工程说明 |
| [SECURITY.md](./SECURITY.md) | 安全策略与漏洞反馈 |
| [SECURITY_BASELINE.md](./SECURITY_BASELINE.md) | 发布前安全基线 |
| [CONTRIBUTING.md](./CONTRIBUTING.md) | 贡献指南 |

## License

GPLv3，详见 [LICENSE](./LICENSE)。

上游项目：[DataEase](https://github.com/dataease/dataease) © FIT2CLOUD 飞致云。
