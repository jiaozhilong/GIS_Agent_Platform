# GIS 知识资产实施任务书落地报告

更新时间：2026-08-13

## 1. 现状分析与完成度

| 任务域 | 原系统状态 | 本次状态 | 说明 |
| --- | --- | --- | --- |
| 5 个业务知识库 | 已有 | 保持 | 未重建 Dataset，未批量重解析既有切片 |
| 知识资产中心 | 部分已有 | 完成 P0 | 增加校验、文档级 Parser、Metadata、异步任务、进度、失败原因、重解析和删除 |
| PPT 资产处理 | 已有页面/媒体抽取 | 完成 P0 | 原始 PPTX 交给 Presentation；平台同步抽取文本、图片、视频关系与页面预览 |
| RAGFlow 适配 | 多处调用 | 完成统一封装 | 上传、更新文档、解析、状态、检索、切片和删除均由适配层调用 |
| GIS 知识检索 | 简单单次检索 | 完成 P1 | Query 扩展、Dataset 路由、Metadata、混合召回、去重排序、媒体和 Evidence |
| Retrieval Evaluation | 无 | 完成框架和首轮实测 | V8 内置 30 条题，异步运行并保存 Recall@5/10、MRR、文档/PPT 页命中率 |
| 需求分析 | 已有 | 复用并统一证据入口 | 项目需求作为最高优先级上下文 |
| 产品匹配 | 已有 | 复用并统一检索 | 产品匹配先查产品技术库，再形成推荐与来源 |
| 方案生成 | 整体一次生成 | 完成 P2 | 9 章节 Blueprint，逐章检索、Evidence Package、生成、引用、编辑、锁定和重生成 |
| PPT Generator | 仅有浏览器导出 | 未纳入本阶段 | 任务书定义为 P3，需另做模板排版引擎 |

## 2. 架构调整

主链路调整为：`Upload API → FileValidator → ParserRouter → PPT Media → RAGFlow 文档级配置/解析 → 异步状态 → Unified Retrieval → Evidence Package → Section Generation`。

平台方案模型和 RAGFlow Assistant 仍可二选一：平台模型启用且健康时用于章节生成，否则由 RAGFlow Assistant 生成；两条生成路径都消费同一套 Evidence Package。

## 3. 数据库 Migration

- `V7__production_knowledge_ingestion.sql`：资产生产元数据、Parser 决策、解析任务、媒体扩展、章节规划和证据字段。
- `V8__add_retrieval_evaluation.sql`：评测题、评测运行、逐题结果；初始化 30 条 GIS 售前问题。

数据库字段保留原表和既有数据，只做向前扩展。

## 4. Parser Route

| 条件 | Parser | 默认策略 |
| --- | --- | --- |
| `.pptx` | Presentation | 强制，且并行做平台媒体处理 |
| FAQ/故障问答 | QA | 问答语义明确时使用 |
| 结构化 Excel | Table | 表格数据优先 |
| 产品手册 DOCX/PDF | General | `docx-manual-enabled` / `pdf-manual-enabled` 默认关闭 |
| 普通 DOCX/PDF/TXT/MD | General | 跨格式兼容默认值 |
| 图片/视频资产说明 | General | 生成结构化说明 DOCX 入库，原媒体保留在资产中心 |

Parser 决策、原因、配置、是否人工覆盖均落库。重新解析会清理该文档自己的旧切片，不操作其他文档。

## 5. RAGFlow API 流程

1. `POST /api/v1/datasets/{datasetId}/documents` 上传原件。
2. `PATCH /api/v1/datasets/{datasetId}/documents/{documentId}` 写入文件级配置。RAGFlow v0.26.4 会在上传 PPTX 时自动设置 Presentation，并禁止再次修改 PPTX 的 `chunk_method`，因此 PPTX 只 PATCH `parser_config` 和 `meta_fields`；其他文档继续写入 `chunk_method`。
3. `POST /api/v1/datasets/{datasetId}/chunks` 启动解析。
4. 轮询文档状态和 chunk 数，更新平台任务进度。
5. `POST /api/v1/retrieval` 做混合检索；结果统一转成 KnowledgeFragment 和 Evidence。
6. 重解析只删除目标文档切片；删除资产才删除目标 RAGFlow 文档。

当前 v0.26.4 镜像 ES 混合检索存在过严的位置断言，本机 Compose 已挂载 `docker/ragflow-patches/es_conn.py` 修复，知识库数据卷未改动。

## 6. PPT 媒体处理

PPTX 经过 ZIP/OOXML 校验后逐页抽取文本和标题，导出图片、识别内嵌/外链视频，生成页面预览并建立 `asset → slide → media` 关系。超过 128MB 的媒体型 PPTX 使用 OOXML 流式处理，避免将数 GB 视频完整装入 JVM 对象图。人工检索和章节 Evidence 可返回资产 ID、PPT 页码、图片/视频和页面预览。

## 7. Retrieval 架构

- Query 扩展：一张图、总体架构、产品和案例等 GIS 语义扩展。
- Dataset Router：PRODUCT/SOLUTION/CASE/TEMPLATE/TROUBLESHOOTING 映射五个业务库。
- Metadata Filter：新资产使用 RAGFlow `meta_fields`；旧生产切片没有 Metadata 时自动兼容回退。
- Hybrid Retrieval：BGE-M3 向量 + 全文关键词；阈值、候选数和向量权重配置化。
- Aggregator/Rerank：多 Query 合并、dataset/document/chunk 去重、原始 Query 和产品实体加权。
- Media Enricher：关联知识资产、PPT 页、预览、图片和视频。
- Evidence Package：方案和人工检索共用，不允许生成器绕过检索服务直接查 RAGFlow。

## 8. Requirement、Product、Solution

- Requirement Analyzer：以当前项目客户需求为最高优先级，已有实现继续使用。
- Product Matcher：基于产品库证据和平台产品能力矩阵，已有实现继续使用。
- Solution Planner：固定 9 类业务章节蓝图，每章包含 purpose、knowledge types、queries 和 generation requirements。
- Solution Generator：逐章检索、逐章生成，事实/产品/案例引用 Evidence；证据不足时进入待确认；历史客户名称、金额、服务器数量、地区和工期禁止串入当前项目。
- 章节工作台：正文编辑保存、锁定/解锁、单章重新检索和生成、证据和 PPT 页追溯。

## 9. 平台 API

- 资产：`GET/POST/DELETE /api/v1/knowledge-assets`、详情、重解析、状态、媒体、预览、下载。
- 检索：`POST /api/v1/knowledge-search`、项目内 retrieval。
- 评测：题库、创建 Run、Run 列表和详情。
- 方案：创建/查询 Run，读取、编辑、锁定和重新生成章节。

## 10. 前端变化

- 知识资产中心新增知识类型、文档类型、行业/GIS 域/产品/版本/地区等元数据。
- 上传立即返回并轮询后台进度，展示 Parser 决策、原因、失败码和重解析入口。
- 方案页面展示逐章状态、Evidence、PPT 页和资产入口，并支持正文编辑、锁定与重生成。

## 11. 配置

- `platform.assets.worker-*`：有界异步解析线程池。
- 平台、RAGFlow `MAX_CONTENT_LENGTH` 和 Nginx `client_max_body_size` 已统一为 5GB；大文件前端超时 2 小时，平台到 RAGFlow 请求超时 1 小时。
- `platform.knowledge.parser.*-manual-enabled`：Manual 灰度开关，默认关闭。
- `platform.knowledge.retrieval.*`：候选数、最终 TopK、阈值、向量权重、关键词和 rerank。
- RAGFlow API Key、Assistant ID、数据库密码和平台模型 Key 均继续使用环境变量/加密配置，不写明文进源码。

## 12. 测试结果

- 后端：`mvn test` 通过，FileValidator 与 ParserRouter 共 5 个自动化用例。
- 前端：`npm run typecheck` 与 `npm run build` 通过；只有既有大 chunk 性能警告。
- Spring Boot：V8 migration、Hibernate validate、真实 8094 启动通过。
- RAGFlow：5 个业务知识库保持；BGE-M3 + ES 混合检索实际返回产品技术证据。
- 资产：现有 5 个资产均保持 READY；未触发生产文档重解析。
- 章节：保存、锁定、锁定时返回 409、解锁通过。
- Retrieval Evaluation：30 条题库和异步运行已验证；首轮 Run `529682ff-a2e5-4047-8fb9-cb851fc1a576` 为 30/30 成功，Recall@5=0.6333、Recall@10=0.6333、MRR=0.5944。
- MinIO/RAGFlow 入库：临时 Markdown 自动走 QA，异步完成 5 个切片，MinIO original 对象存在，检索首条相关命中约 0.663；删除 API 完成后 RAGFlow 文档、平台记录和对象均清理，资产数量恢复为 5。
- 大型 PPTX：使用 286MB、49 页、内嵌 6 个视频的真实文件完成上传、49 页/89 图片/6 视频流式识别、Presentation 解析、BGE-M3 向量化、49 Chunk 建索引和页码检索；该项目资产保留在平台、RAGFlow 和 MinIO 中。另一个 371MB、53 页、含约 240MB 单视频的 PPTX 已通过平台流式解析测试，本地原始 PPTX 仅作为验证资料，不纳入 Git。
- 收尾清理：删除 1 条由旧版 PPTX `chunk_method` 更新行为产生的 ERROR 测试资产，并通过平台删除链路同步清理其 RAGFlow 文档、MinIO 对象、数据库关联记录和本地缓存；最终知识资产为 5 条 READY、0 条 ERROR。

## 13. Manual vs General

生产默认仍为 General。尚未直接对 01 产品技术库做重解析；正式对比需要业务方选定 3–5 份可复制的典型产品 DOCX，在两个临时 Evaluation Dataset 中分别解析并标注 10–20 条精确问题。评测模块和开关已准备好，但在没有 gold label 前不凭感觉启用 Manual。

## 14. 兼容、已知问题和未完成项

- 既有 2418 个产品切片和其他知识库内容不删除、不批量重解析；旧切片无 Metadata 时检索自动回退。
- 新上传/重解析资产会写入现有 MinIO 的 `gis-knowledge-original`、`gis-knowledge-media`、`gis-knowledge-preview` 三个独立 bucket，并保留本地缓存兼容既有下载接口；历史 5 个资产可在下次重解析时无损补齐对象键。平台不依赖 RAGFlow 内部 bucket 目录结构。
- `Precision@10` 和精确 PPT Slide Recall 需要人工补完 expectedDocument/expectedSlide gold label；V8 已提供字段和运行框架。
- P3 原生 DOCX/PPTX/PDF 排版生成、模板匹配和图片编排未实施。
