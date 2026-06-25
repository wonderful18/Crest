# Crest 文档

本文是 Crest 文档入口。根目录 README 只保留项目概览和常用入口，详细说明按主题维护在本目录和部署目录中。

## 部署运维

| 文档 | 内容 |
| --- | --- |
| [单机安装与离线包](../installer/README.md) | Docker 单机部署、配置项、备份、升级和离线包制作 |
| [Kubernetes 部署](../deploy/kubernetes/README.md) | 内置 MySQL、外部 MySQL、Secret、探针和可观测性部署 |
| [前后端分离部署](../deploy/split/README.md) | Compose 方式验证前后端分离部署 |
| [OceanBase 支持](./oceanbase-support.md) | Crest 系统库和业务数据源接入 OceanBase 的配置与验收 |
| [可观测性](./observability.md) | Prometheus、Grafana、指标、告警和排障 |

## 产品能力

| 文档 | 内容 |
| --- | --- |
| [数据血缘](./data-lineage.md) | 字段级血缘范围、入口、数据来源和接口 |
| [单点登录](./sso.md) | OIDC / OAuth2 授权码模式、用户映射和安全要求 |
| [平台管理](./platform-management.md) | 用户、组织、角色、菜单权限和资源权限 |
| [研发效能指标](./engineering-efficiency.md) | 研发效能演示资源的指标口径和数据接入要求 |

## 开发和发布

| 文档 | 内容 |
| --- | --- |
| [开发说明](./development.md) | 仓库结构、工具链、构建命令、迁移规则和提交检查 |
| [发布管理](./release-process.md) | 分支、补丁、SQL、发布包和 release 检查 |
| [前端工程](../core/core-frontend/README.md) | 前端构建、视觉约定、品牌资源和页面规则 |
| [发布说明](./release/v2.0.1.md) | 当前稳定版本发布说明 |

历史版本发布说明保留在 [release](./release) 目录。过程类审查报告、临时计划和本地环境记录不放入公开文档。
