# GIS Agent Platform

面向 GIS 售前与解决方案团队的 AI 工作平台。前端和后端由同一个 Git 仓库统一管理：

```text
GIS_Agent_Platform/
├─ frontend/        Vue 3 + TypeScript + Vite
├─ backend/         Spring Boot 3 + PostgreSQL
├─ docs/            OpenAPI 与后端编排文档
├─ scripts/         设计验收与辅助脚本
└─ pnpm-workspace.yaml
```

开发模式下仍然有两个运行进程：Vite 使用 `5173`，Spring Boot 使用 `8080`。前端通过 Vite 代理访问 `/api`，浏览器不需要直接处理跨域，也不会接触 RAGFlow API Key。

## 环境要求

- Node.js 20+
- pnpm
- Java 17
- PostgreSQL 14+
- RAGFlow API：`http://localhost:9380`
- BGE-M3 服务：`http://localhost:8001`

## 前端安装与启动

进入前端目录执行：

```powershell
cd frontend
pnpm install
Copy-Item .env.example .env.local
pnpm dev
```

前端地址：`http://localhost:5173`。开发服务器会把 `/api` 请求代理到 `http://localhost:8080`。

## 后端启动

使用 IDEA 打开项目根目录或 `backend` 目录：

1. 将项目 SDK 设置为 Java 17。
2. 找到 `backend/src/main/java/com/jiaozhilong/gisagent/GisAgentPlatformApplication.java`。
3. 创建 Spring Boot 运行配置，并将 Active profiles 设置为 `local`。
4. 运行 `GisAgentPlatformApplication`。

后端本机配置放在 Git 忽略的 `backend/src/main/resources/application-local.yml` 中，或者通过环境变量配置数据库、JWT、RAGFlow API Key 和 Assistant ID。具体配置见 [backend/README.md](backend/README.md)。

后端地址：`http://localhost:8080`，健康检查：`http://localhost:8080/api/v1/health`。

不使用 IDEA 时也可以执行：

```powershell
cd backend
.\mvnw-local.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

## 构建与测试

```powershell
cd frontend
pnpm build
pnpm typecheck

cd ../backend
.\mvnw-local.cmd test
.\mvnw-local.cmd -DskipTests package
```

## 接口契约

- OpenAPI：`docs/api/openapi.yaml`
- 前端类型：`frontend/src/api/contracts.ts`
- 后端统一前缀：`/api/v1`
- API Key 只保存在后端本地配置或环境变量中，不进入前端和 Git

远端仓库：<https://github.com/jiaozhilong/GIS_Agent_Platform.git>
