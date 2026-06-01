# 前后端分离部署

Crest 的发布镜像拆成两个容器：

- `crest-service`：后端 Spring Boot 服务，监听 `8100`；
- `crest`：前端 Nginx 服务，监听容器内 `8080`，对外暴露 Web 端口，并把 `/api/v1/` 与 `/websocket` 转发到后端。

这种方式便于独立替换后端或前端镜像，也方便本地调试后端时直接复用同一套前端资源。

## 本地验证

```bash
cd core/core-frontend
pnpm install --frozen-lockfile
pnpm run build:base

cd ../..
mvn -s .mvn/settings.xml -pl :core-backend -am clean package -Pstandalone -DskipTests -Dmaven.test.skip=true -Dcrest.copy.frontend.skip=true

docker build -f Dockerfile.backend -t crest-service:split .
docker build -f Dockerfile.frontend -t crest-web:split .

CREST_WEB_PORT=18100 docker compose -f deploy/split/docker-compose.yml up -d
```

访问：

```text
http://127.0.0.1:18100/
```

默认账号：

```text
用户名：admin
密码：admin
```

停止验证环境：

```bash
docker compose -f deploy/split/docker-compose.yml down -v
```
