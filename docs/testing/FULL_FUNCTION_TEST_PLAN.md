# GIS Agent Platform 全功能测试计划

## 测试目标

验证 Vue 前端、Spring Boot 后端、PostgreSQL、RAGFlow、BGE-M3、RAGFlow Assistant 与平台生成模型的真实链路，不使用前端 Mock 数据。自动化测试使用独立临时端口，避免影响开发者当前运行的 8080/5173 进程。

## 测试数据

- 管理员：`admin`（密码由本地初始化配置管理）
- 全流程项目：`系统全流程联调项目-<时间>`，覆盖自然资源一张图、二三维 GIS、空间分析、服务共享、权限审计和联合方案生成
- 联调用户：`e2e_<时间>`，完成新增、编辑、角色分配、密码重置和停用
- RAGFlow 资料：5 个业务知识库中的 10 份真实文档及 2374 个切片
- 上传资料：[ragflow-local-troubleshooting.md](../../test-data/ragflow-local-troubleshooting.md)

## 模块与验收点

| 模块 | 主要验收点 | 自动化方式 |
| --- | --- | --- |
| 认证 | 管理员登录、JWT、当前用户 | API + 浏览器 |
| 用户权限 | 列表、筛选、新增、编辑、角色、重置密码、启停 | API + 浏览器 |
| 弹窗 | 项目、用户、知识库弹窗点击遮罩不关闭 | 浏览器 |
| Agent 总览 | 真实文档统计、项目阶段统计、知识库状态、可点击项目网络和自然语言任务路由 | API + 浏览器 |
| 项目工作台 | 新建、编辑、删除、详情及需求/产品/检索/方案五阶段一体化工作台 | API + 浏览器 |
| 需求分析 | 基于项目真实需求生成要点、维度和推荐产品 | API + 浏览器 |
| 产品匹配 | 基于超图官方 2026 产品体系展示 6 类 22 款产品；图节点与推荐复选框双向联动，详情信息坞不遮挡产品图 | API + 浏览器 |
| 项目场景 | 2D/3D 切换、业务图层显隐、场景复位、全屏 | 浏览器 + 构建检查 |
| 知识探索 | 独立页面调用 RAGFlow retrieval 与 BGE-M3，支持知识库范围、TopK、阈值及来源定位 | API + 浏览器 |
| 知识库 | 同步 Dataset、读取文档、上传并触发解析、新建知识库 | API + 浏览器 |
| GIS 知识资产 | 业务格式边界、PPTX 逐页文本/图片/视频/预览、RAGFlow 手工切片、来源增强检索、原件与媒体读取 | API + 浏览器 |
| 模型路由 | RAGFlow 内部配置只读检测；平台模型支持当前参数测试、加密保存、启停、状态持久化与方案路由 | API + 浏览器 |
| 联合方案 | 先检索所选知识库，再按路由使用平台 GPT 类模型或 RAGFlow Assistant 生成，保存引用与覆盖率 | API + 浏览器 |
| 生成记录 | 查询任务状态、模型、正文、引用和下载正文 | API + 浏览器 |
| Retrieval Evaluation | 30 条 GIS 问题、异步 Run、Recall@5/10、MRR、文档/PPT 页命中率 | API + PostgreSQL |

## 执行命令

```powershell
cd E:\RAGFlow\GIS_Agent_Platform\backend
mvn -s .mvn\settings.xml test

cd E:\RAGFlow\GIS_Agent_Platform\frontend
npm run build

cd E:\RAGFlow\GIS_Agent_Platform
node scripts\e2e-smoke.cjs
```

浏览器自动化结果写入被 Git 忽略的 `test-artifacts/e2e-smoke-result.json`，页面截图写入 `test-artifacts/full-function-smoke.png`。

## 2026-08-09 实测结果

- RAGFlow 同步：5 个业务知识库；上传验证后共有 10 份文档、2374 个切片。
- 知识库管理：临时创建接口成功并完成清理；故障排查库真实上传 Markdown，解析状态 READY，生成 5 个切片。
- 总览统计：知识资产显示 10 份文档，与 RAGFlow 文档总数一致，不再把 2374 个切片误算为文档。
- 模型检测：RAGFlow、RAGFlow Assistant、BGE-M3 均为 HEALTHY；平台模型改用真实 Chat Completions 测试，不依赖供应商可选的 `/models` 接口。当前已保存参数实测为 HEALTHY，API Key 留空保存时可正确保留。
- 需求分析：SUCCEEDED，6 个需求要点，完整度 77%。
- 产品体系：按[超图官方 SuperMap GIS 2026 产品体系](https://help.supermap.com/iManager_K8S/zh/GettingStarted/ProductLine.htm)收录 6 类 22 款产品；实际项目匹配 9 款，默认、命中、推荐和选中状态可交互区分。
- 知识探索：独立检索页面命中 5 个真实切片，返回文档、页码、相似度和原文，接口耗时约 407ms。
- 联合方案：SUCCEEDED，正文约 9955 字符，22 条引用，证据覆盖率 100%。
- 项目管理：新建、编辑、删除均已通过真实 PostgreSQL API 验证；左侧不再重复显示项目阶段，阶段统一进入项目工作台。
- 浏览器自动化：28 个页面/交互检查全部通过，新增平台模型“当前参数测试/保存重载”和产品组合双向联动验证，控制台与 HTTP 错误为 0。
- 弹窗行为：项目、用户和知识库弹窗只能通过“关闭/取消”按钮退出，点击遮罩不再误关闭。
- Agent 体验：总览改为任务指令入口、实时项目节点网络、阶段过滤和项目检查器；产品宇宙与匹配结果双向联动。

## 当前边界

- 方案正文已经由“RAGFlow 知识检索 + 生成模型”真实生成；平台模型未启用时走 RAGFlow Assistant，启用并检测通过后改走独立 OpenAI 兼容模型。当前 Word 按兼容 `.doc` 导出，原生 `.docx`、`.pptx`、`.pdf` 排版生成属于后续文档引擎能力。
- 企业微信身份认证尚未接入，登录页入口已禁用并标记“未配置”。
- 当前只有系统内置角色，权限固定且不可编辑；普通用户的新增、编辑、角色分配、密码重置和启停均已可用。
- 平台生成模型需要管理员在“模型路由”中填写实际 API Key、OpenAI 兼容 Base URL 和模型名后才能完成供应商侧健康检查；API Key 使用 AES-GCM 加密入库且不会回显明文。

## 2026-08-12 任务书增量回归

- 新增 V7/V8 migration，Spring Boot 使用本地 PostgreSQL 实际启动且 Hibernate validate 通过。
- FileValidator、PPTX OOXML 校验、Parser Router 自动路由共 5 个后端测试通过。
- 5 个现有业务知识库和 5 个 READY 平台资产保持不变，未批量重解析生产资料。
- 修复 RAGFlow v0.26.4 Elasticsearch 混合检索断言后，平台通过 BGE-M3 实际返回 SuperMap ImageX/iServer 产品技术片段。
- 方案章节保存、锁定、锁定时禁止重生成（HTTP 409）和解锁接口通过。
- V8 初始化 30 条真实 GIS 售前问题，评测 Run 异步执行并持久化逐题结果与指标。
- 首轮真实评测 30/30 请求成功，Recall@5/10 为 0.6333，MRR 为 0.5944；PPT 页 gold label 尚未录入，不把“无样本”误报为 0% 命中。
- 临时 Markdown 资产经 QA Parser 异步生成 5 个切片，MinIO 对象、RAGFlow 检索和资产关联验证通过；删除后 RAGFlow、PostgreSQL、MinIO 同步清理。
- 前端 `vue-tsc` 和 Vite production build 通过；保留既有大 chunk 构建警告。

## 2026-08-11 知识资产中心实测结果

- 使用真实 3 页 PPTX 验证增强解析：得到 3 个页面索引、2 张提取图片、1 条外链视频关系和 3 张页面预览。
- 目标知识库为 `manual` 切片模式；平台通过 RAGFlow 文档切片 API 按页写入 3 个可检索切片，不再出现“文档 READY 但切片为 0”。
- 全局知识检索实际命中 3 个页面切片，首条命中第 2 页，相似度约 0.56；返回资产 ID、PPT 页码、页面标题和关联图片，第 3 页同时返回关联视频。
- 页面预览和提取媒体接口均使用 JWT 鉴权，实测返回 HTTP 200 和 `image/png`。
- 浏览器全功能回归通过 30 项页面/交互检查，覆盖资产边界文案、资产详情、注入弹窗遮罩锁定、项目工作台、检索、方案、模型和用户管理；控制台与 HTTP 错误为 0。
- 模型配置接口对历史不可解密凭据改为可恢复状态，不再导致整个页面 500；本机当前平台模型需管理员重新输入一次 API Key 并保存，之后使用稳定本地加密密钥跨重启保留。
