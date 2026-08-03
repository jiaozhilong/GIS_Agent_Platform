# GIS Agent Platform Backend

Spring Boot 3 + PostgreSQL 后端。当前已实现 JWT 登录、用户/角色/权限管理，以及 RAGFlow 知识库与大模型联合生成任务的首版编排。

## 本机配置

- Java：17
- PostgreSQL：`localhost:5432`
- 数据库：`gis_agent_platform`
- 数据库用户名：`postgres`
- RAGFlow：`http://localhost:9380`

数据库密码通过 `GIS_AGENT_DB_PASSWORD` 环境变量传入。本机可在 Git 忽略的
`src/main/resources/application-local.yml` 中配置，并使用 `local` profile 启动。
JWT 签名密钥和初始管理员密码分别通过 `GIS_AGENT_JWT_SECRET`、
`GIS_AGENT_ADMIN_PASSWORD` 配置，不在仓库中保存默认值。

首次启动由 Flyway 自动建表并创建管理员：

- 账号：`admin@gis-agent.local` 或 `admin`
- 密码：通过 `GIS_AGENT_ADMIN_PASSWORD` 环境变量或本机 `local` profile 配置

## 启动

```powershell
cd E:\RAGFlow\GIS_Agent_Platform\backend
.\mvnw-local.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

也可先打包再运行：

```powershell
.\mvnw-local.cmd -DskipTests package
& 'D:\java\jdk17\bin\java.exe' -jar '.\target\gis-agent-platform-backend-0.1.0-SNAPSHOT.jar'
```

## RAGFlow 联合生成配置

后端不会在浏览器保存 RAGFlow API Key。启动前设置：

```powershell
$env:RAGFLOW_BASE_URL='http://localhost:9380'
$env:RAGFLOW_API_KEY='<your-ragflow-api-key>'
$env:RAGFLOW_ASSISTANT_ID='绑定知识库和 DeepSeek 的 chat-assistant-id'
```

生成时优先使用知识库证据；知识库缺失的通用方案内容由模型补全；事实性内容无证据时标记为“待确认”。详细规则见 `../docs/backend/API_AND_ORCHESTRATION.md`。
