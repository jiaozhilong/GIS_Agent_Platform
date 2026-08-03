# GIS Agent Platform

面向 GIS 售前与解决方案团队的 AI 工作平台。前端按照素材包的深色数字孪生设计实现，主流程覆盖：项目创建、需求分析、产品匹配、RAGFlow 知识检索、引用确认、DeepSeek 方案生成、PPT/Word/PDF 交付。

## 技术栈

- Vue 3 + TypeScript + Vite
- Three.js：登录能量核心、产品能力星图
- Cesium：项目作战室三维 GIS 场景
- ECharts：总览、任务进度、需求雷达图
- Axios + TypeScript 契约；Spring Boot 3 + PostgreSQL 后端

## 本地运行

```powershell
pnpm install
pnpm dev
```

本机 `.env.local` 已切换至真实 Spring Boot 接口。未创建 `.env.local` 时仍可使用 mock 接口体验主链路：

- 账号：`admin@gis-agent.local`
- 密码：通过本机 `GIS_AGENT_ADMIN_PASSWORD` 配置

切换 Spring Boot 后端时复制 `.env.example` 为 `.env.local`，设置：

```text
VITE_API_BASE_URL=/api/v1
VITE_USE_MOCKS=false
```

后端启动与 RAGFlow 配置见 `backend/README.md`。

## 接口契约

接口唯一标准位于 `docs/api/openapi.yaml`，前端对应类型位于 `src/api/contracts.ts`。后端必须保持：

- 统一 `/api/v1` 前缀；
- 统一 `ApiResponse<T>` 响应包装；
- 枚举值使用大写英文，不用中文状态字符串；
- AI 任务使用 `PENDING/RUNNING/SUCCEEDED/FAILED/CANCELLED`；
- 检索结果必须返回知识库、文档、chunk、页码和元数据，以保证引用可追溯；
- API Key 仅保存在后端，前端只接收脱敏值。

## Git

远端仓库：`https://github.com/jiaozhilong/GIS_Agent_Platform.git`

当前网络无法访问 GitHub 时，可继续本地开发；网络恢复后推送即可。
