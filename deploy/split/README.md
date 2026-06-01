# 前后端分离部署

本目录用于验证 Crest 的前后端分离运行形态。生产发布时，后端服务、前端网关和元数据库分别运行在独立容器中：

| 服务 | 容器 | 说明 |
| --- | --- | --- |
| `crest-service` | Spring Boot | 提供 `/api/v1/`、`/websocket`、OpenAPI 文档和后台任务 |
| `crest` | Nginx | 提供前端静态资源，转发后端接口和 WebSocket |
| `mysql-crest` | MySQL 8.0 | 保存元数据、系统配置、演示资源和任务状态 |

前端容器只暴露 Web 端口。后端和 MySQL 只在 Compose 网络内访问，外部反向代理不应直接暴露后端端口。

## 本地构建

```bash
cd core/core-frontend
pnpm install --frozen-lockfile
pnpm run build:base

cd ../..
mvn -s .mvn/settings.xml -pl :core-backend -am clean package -Pstandalone -DskipTests -Dmaven.test.skip=true -Dcrest.copy.frontend.skip=true

docker build -f Dockerfile.backend -t crest-service:split .
docker build -f Dockerfile.frontend -t crest-web:split .
```

## 启动

```bash
CREST_WEB_PORT=18100 docker compose -f deploy/split/docker-compose.yml up -d
```

访问地址：

```text
http://127.0.0.1:18100/
```

默认账号：

```text
用户名：admin
密码：admin
```

本地验证使用固定密码和固定加密参数。生产环境必须改为随机值，并在首次登录后修改管理员密码。

## 安全边界

拆分部署默认启用以下容器加固项：

- 前端和后端容器均以非 root 用户 `10001:10001` 运行；
- Compose 显式指定运行用户，避免镜像默认值被误改后扩大权限；
- 前端和后端容器启用只读根文件系统；
- 前端和后端容器删除全部 Linux capability；
- 前端和后端容器启用 `no-new-privileges`；
- 前端和后端容器启用进程数和打开文件数边界；
- 容器标准输出日志启用大小和文件数滚动，避免占满宿主机磁盘；
- 运行时写入目录通过 volume 或 tmpfs 显式挂载；
- Nginx 不暴露版本号，静态资源和 HTML 使用不同缓存策略；
- Nginx 为前端入口和静态资源发送点击劫持、MIME 嗅探、CSP、引用来源和浏览器权限相关安全头；
- Nginx 为接口、WebSocket 和文档入口设置独立超时；
- Nginx 为请求写入 `X-Request-Id`，便于网关、后端和日志关联；
- Nginx 在 Docker 网络内按固定周期解析后端服务名，后端容器重建后不需要重启前端网关；
- MySQL 服务不暴露宿主机端口。

`/doc.html`、`/v3/api-docs` 和 `/swagger-ui` 会转发到后端，便于本地调试接口。生产环境如果需要开放接口文档，应放在内网或网关白名单后面。

## Nginx 说明

前端主配置位于 `deploy/split/nginx/nginx.conf`，站点配置位于 `deploy/split/nginx/crest.conf`。主要策略：

| 项目 | 策略 |
| --- | --- |
| 健康检查 | `/healthz` 返回 `204` |
| 上传大小 | `client_max_body_size 500m`，与后端上传限制保持一致 |
| API 转发 | `/api/v1/` 转发到 `crest-service:8100/api/v1/` |
| 请求追踪 | 生成并透传 `X-Request-Id` |
| 网关头 | 转发时保留浏览器原始 `Host`、外部端口和可信 HTTPS 网关协议，后端重定向不降级 |
| 大响应处理 | API 响应不落 Nginx 临时文件，避免大导出占用网关磁盘 |
| WebSocket | `/websocket` 保持长连接，读写超时 1 小时 |
| HTML 缓存 | `index.html` 和路由回退不缓存，避免升级后加载旧入口 |
| 静态资源缓存 | JS、CSS、图片和字体缓存 30 天 |
| 服务解析 | 通过 Docker 内置 DNS 定期解析后端服务，降低容器重建后的 502 风险 |
| Source Map | `*.map` 默认不对外提供 |
| 隐藏文件 | 拒绝访问隐藏文件，保留 `.well-known` 例外 |

如果生产环境由外部网关终止 HTTPS，建议在外部网关配置 HSTS、TLS 版本、证书轮换、访问日志脱敏和 IP 白名单。容器内 Nginx 不默认发送 HSTS，避免在 HTTP 本地验证和内网代理场景中产生误配置。

## 验证清单

启动后建议执行：

```bash
docker compose -f deploy/split/docker-compose.yml ps
docker exec split-crest-web nginx -t
curl -sSI http://127.0.0.1:18100/healthz
curl -sS -H 'Origin: http://127.0.0.1:18100' http://127.0.0.1:18100/api/v1/public-key
docker logs split-crest-service --since 5m | grep -E 'WARN|ERROR|Exception' || true
docker logs split-crest-web --since 5m | grep -Ei 'warn|error|exception' || true
```

页面验证至少覆盖：

- 登录和退出；
- 工作台加载；
- 数据源列表和数据源校验；
- 数据集预览、自定义 SQL 预览和导出；
- 仪表盘打开、图表查看数据、分享入口；
- 数据大屏打开；
- 数据血缘打开、搜索和节点展开；
- 系统设置保存；
- `/doc.html` 能打开接口文档。

## 停止和清理

停止服务但保留数据：

```bash
docker compose -f deploy/split/docker-compose.yml down
```

清理本地验证数据：

```bash
docker compose -f deploy/split/docker-compose.yml down -v
```

执行 `down -v` 会删除本地 MySQL、缓存、日志和上传目录，只适合开发验证环境。
