# 后端接口与 RAGFlow 联合生成设计

## 1. 系统边界

GIS Agent Platform 后端负责用户权限、项目上下文、生成任务编排、版本、审计和 PostgreSQL 持久化。RAGFlow 负责知识库检索、BGE-M3 向量召回、已配置大模型推理、引用返回和 Agent/Chat 会话。

方案生成不是“平台检索后再单独调用模型”，而是由后端调用 RAGFlow Chat/Agent 完成一次原生 RAG 推理。平台只对输入、阶段、质量规则和结果进行编排。

## 2. 方案内容来源规则

每个方案段落必须记录 `sourceType`：

- `KNOWLEDGE_BASE`：主要内容来自知识库证据，必须有引用。
- `MODEL_GENERATED`：知识库无直接材料，由模型生成通用建议，必须显式标识。
- `HYBRID`：知识证据与模型组织、推演共同形成。
- `PENDING_CONFIRMATION`：事实性内容没有可靠证据，禁止模型编造，留待人工补充。

以下内容采用严格证据策略：产品型号和参数、历史案例数据、政策规范、客户现状、报价与工期承诺。知识库无证据时必须返回 `PENDING_CONFIRMATION`。

以下内容允许模型补全：章节结构、建设思路、技术路线说明、实施建议、风险与保障措施、段落衔接和语言润色。

## 3. RAGFlow 联合生成流程

1. 后端读取项目需求、行业、地区、已选知识库和生成策略。
2. 创建 `solution_generation_run`，状态为 `PENDING`。
3. 调用已配置的 RAGFlow Chat Assistant 或 Agent；该 Assistant 在 RAGFlow 内绑定目标 datasets、BGE-M3 和 DeepSeek。
4. 第一阶段生成章节规划、检索问题和各章节证据要求。
5. 第二阶段逐章节调用 RAGFlow 会话，由 RAGFlow 在生成时同步检索知识库并返回答案和引用。
6. 后端按引用覆盖率判定 `sourceType`；事实章节无证据时改为 `PENDING_CONFIRMATION`。
7. 第三阶段由 RAGFlow 大模型进行跨章节一致性检查、去重和语言统一。
8. 保存正文、引用快照、RAGFlow 会话 ID、模型、耗时、证据覆盖率和版本。

RAGFlow v0.26.4 适配层统一封装在 `RagflowGateway`，避免业务代码依赖其具体响应结构。API Key、Chat/Agent ID 和 dataset ID 只存后端配置，不下发浏览器。

## 4. API 分组

统一前缀 `/api/v1`，统一返回 `ApiResponse<T>`。认证使用 Bearer JWT。

### 认证

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/login` | 账号或邮箱登录 |
| GET | `/auth/me` | 当前用户、角色和权限 |
| POST | `/auth/logout` | 前端清理令牌，后端记录审计 |

### 用户、角色、权限

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/users` | 分页、关键字、状态、角色筛选 |
| POST | `/users` | 创建用户 |
| GET | `/users/{userId}` | 用户详情 |
| PUT | `/users/{userId}` | 修改基本信息 |
| PATCH | `/users/{userId}/status` | 启用、停用、锁定 |
| PUT | `/users/{userId}/roles` | 分配角色 |
| POST | `/users/{userId}/reset-password` | 管理员重置密码 |
| GET | `/roles` | 角色列表和权限摘要 |
| GET | `/permissions` | 权限字典 |
| PUT | `/roles/{roleId}/permissions` | 修改自定义角色权限 |

### 项目与 AI 任务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST | `/projects` | 项目列表、创建 |
| GET/PUT | `/projects/{projectId}` | 项目详情、修改 |
| POST | `/projects/{projectId}/requirement-analysis` | RAGFlow 辅助需求分析 |
| POST | `/projects/{projectId}/product-matches` | 产品能力匹配 |
| POST | `/projects/{projectId}/retrievals` | 独立知识检索与调试 |

### RAGFlow 原生方案生成

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/projects/{projectId}/solution-runs` | 启动知识库与模型联合生成 |
| GET | `/solution-runs/{runId}` | 查询状态、章节、引用和证据覆盖率 |
| POST | `/solution-runs/{runId}/cancel` | 取消任务 |
| POST | `/solution-runs/{runId}/sections/{sectionId}/regenerate` | 按原证据策略重生成章节 |
| POST | `/solution-runs/{runId}/publish` | 固化为方案版本 |
| POST | `/proposals/{proposalId}/exports` | 导出 DOCX/PPTX/PDF |

### 知识库与配置

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/knowledge-bases` | RAGFlow dataset 映射 |
| POST | `/knowledge-bases/{id}/documents` | 上传并触发 RAGFlow 解析 |
| GET/PUT | `/model-configs` | RAGFlow、DeepSeek、BGE-M3 配置 |
| POST | `/model-configs/{provider}/test` | 连通性测试 |

### GIS 行业知识资产中心

知识资产中心是 RAGFlow 上层的业务资产化入口，不承担 GIS 数据、CAD、遥感影像或三维模型管理。原始文件、媒体和预览写入现有 MinIO 的三个平台独立逻辑 bucket，本地目录作为兼容缓存；检索文本和切片写入 RAGFlow，页面与媒体关系、对象键保存在 PostgreSQL。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST | `/knowledge-assets` | 查询或注入 PDF、Word、PPT/PPTX、Excel、图片和视频资产 |
| GET | `/knowledge-assets/summary` | 文档、图片、视频、PPT 页面及可用资产统计 |
| GET | `/knowledge-assets/{id}` | 资产详情、PPT 页面索引及关联媒体 |
| DELETE | `/knowledge-assets/{id}` | 删除平台资产和对应 RAGFlow 文档（处理中禁止删除） |
| POST | `/knowledge-assets/{id}/sync` | 重新同步 RAGFlow |
| POST | `/knowledge-assets/{id}/reparse` | 自动路由或管理员指定 Parser 后异步重解析 |
| GET | `/knowledge-assets/{id}/parse-status` | 查询解析阶段、进度和失败原因 |
| GET | `/knowledge-assets/{id}/download` | 下载原始业务资产 |
| GET | `/knowledge-assets/{id}/pages/{page}/preview` | 获取 PPT 页面预览 |
| GET | `/knowledge-assets/media/{mediaId}` | 获取提取图片或内嵌视频 |
| POST | `/knowledge-search` | 跨项目知识检索，返回文本、PPT 页面来源及关联图片/视频 |

PPTX 采用增强链路：逐页抽取文本、图片、视频关系和页面预览；原始 PPTX 上传 RAGFlow，并在文档级强制使用 `presentation`。普通 PDF、DOCX、Excel、TXT 和 Markdown 由平台文档级 Parser Router 决策，不再受 Dataset 单一默认解析方式限制；旧版 `.ppt` 明确拒绝并提示转为 `.pptx`。

### Retrieval Evaluation

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/retrieval-evaluations/cases` | 30 条 GIS 售前评测题及 gold label |
| POST | `/retrieval-evaluations/runs` | 异步执行真实 RAGFlow 检索评测 |
| GET | `/retrieval-evaluations/runs` | 最近评测运行与指标 |
| GET | `/retrieval-evaluations/runs/{id}` | 逐题命中排名和错误详情 |

### 章节工作台

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/PATCH | `/solution-sections/{sectionId}` | 读取或保存章节正文 |
| POST | `/solution-sections/{sectionId}/lock` | 锁定/解锁章节 |
| POST | `/solution-sections/{sectionId}/regenerate` | 使用同一 Evidence 流程异步重生成单章 |

## 5. 首批后端开发范围

本阶段实际实现：健康检查、JWT 登录、当前用户、用户分页与增改、状态、角色分配、密码重置、角色和权限查询、数据库迁移、初始化管理员，以及 RAGFlow 联合生成任务的数据模型和适配器接口。项目和 AI 业务接口按同一契约继续实现。
