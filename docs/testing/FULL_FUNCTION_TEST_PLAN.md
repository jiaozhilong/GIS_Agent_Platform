# GIS Agent Platform 全功能测试计划

## 测试目标

验证 Vue 前端、Spring Boot 后端、PostgreSQL、RAGFlow、BGE-M3 与 DeepSeek 的真实链路，不使用前端 Mock 数据。测试环境采用本地 `local` profile，自动化后端端口为 8081，前端端口为 5174，避免影响开发者当前运行的 8080/5173 进程。

## 测试数据

- 管理员：`admin`（密码由本地初始化配置管理）
- 全流程项目：`系统全流程联调项目-<时间>`，覆盖自然资源一张图、二三维 GIS、空间分析、服务共享、权限审计和联合方案生成
- 联调用户：`e2e_<时间>`，完成新增、编辑、角色分配、密码重置和停用
- RAGFlow 资料：产品技术知识库中的 9 份 SuperMap 2025 文档及 2369 个真实切片
- 上传资料：[ragflow-local-troubleshooting.md](../../test-data/ragflow-local-troubleshooting.md)

## 模块与验收点

| 模块 | 主要验收点 | 自动化方式 |
| --- | --- | --- |
| 认证 | 管理员登录、JWT、当前用户 | API + 浏览器 |
| 用户权限 | 列表、筛选、新增、编辑、角色、重置密码、启停 | API + 浏览器 |
| 弹窗 | 项目、用户、知识库弹窗点击遮罩不关闭 | 浏览器 |
| 看板 | PostgreSQL 项目/任务统计与 RAGFlow 切片统计 | API + 浏览器 |
| 项目 | 新建、列表、详情、流程阶段推进 | API + 浏览器 |
| 需求分析 | 基于项目真实需求生成要点、维度和推荐产品 | API + 浏览器 |
| 产品匹配 | 使用真实 RAGFlow 检索证据匹配产品能力 | API + 浏览器 |
| 项目场景 | 2D/3D 切换、业务图层显隐、场景复位、全屏 | 浏览器 + 构建检查 |
| 知识检索 | 调用 RAGFlow retrieval 与 BGE-M3，返回文档、切片和相似度 | API + 浏览器 |
| 知识库 | 同步 Dataset、读取文档、上传并触发解析、新建知识库 | API + 浏览器 |
| 模型配置 | RAGFlow、DeepSeek Assistant、BGE-M3 实时健康检测 | API + 浏览器 |
| 联合方案 | 先检索所选知识库，再由 Assistant/DeepSeek 生成，保存引用与覆盖率 | API + 浏览器 |
| 生成记录 | 查询任务状态、模型、正文、引用和下载正文 | API + 浏览器 |

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
- 模型检测：RAGFlow、DeepSeek、BGE-M3 均为 HEALTHY。
- 需求分析：SUCCEEDED，6 个需求要点，完整度 77%。
- 产品匹配：返回 4 个产品，首选 SuperMap iServer。
- 知识检索：命中 5 个真实切片，首条来自《SuperMap iServer 2025 用户手册》，接口耗时约 466ms。
- 联合方案：SUCCEEDED，正文约 9955 字符，22 条引用，证据覆盖率 100%。
- 浏览器自动化：22 个页面/交互检查全部通过，控制台与 HTTP 错误为 0。
- 弹窗行为：项目、用户和知识库弹窗只能通过“关闭/取消”按钮退出，点击遮罩不再误关闭。
- 产品分类、场景图层显隐、场景复位和全屏已补成真实前端交互；缺少产品主数据支撑的部署方式、版本、价格筛选已明确禁用。

## 当前边界

- 方案正文已经由“RAGFlow 知识检索 + DeepSeek Assistant”真实生成；当前 Word 按兼容 `.doc` 导出，文本可直接下载，原生 `.docx`、`.pptx`、`.pdf` 排版生成属于后续文档引擎能力。
- 企业微信身份认证尚未接入，登录页入口已禁用并标记“未配置”。
- 当前只有系统内置角色，权限固定且不可编辑；普通用户的新增、编辑、角色分配、密码重置和启停均已可用。
- 部署方式、版本、价格字段尚未进入产品主数据，因此相关筛选不会伪造结果；待产品库补齐字段后再开放。
