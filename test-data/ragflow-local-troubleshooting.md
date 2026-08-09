# RAGFlow 本地服务连通性排查手册

## 适用范围

本文用于 GIS Agent Platform 本地环境的 RAGFlow、BGE-M3 和 Spring Boot 联调排查。当前约定的 RAGFlow API 地址为 `http://localhost:9380`，Web 页面地址为 `http://localhost:8088`，BGE-M3 的 OpenAI 兼容接口为 `http://localhost:8001/v1`。

## RAGFlow 页面出现 502

先确认 Docker Desktop 已启动，再检查 RAGFlow、MySQL、Elasticsearch、MinIO、Redis 等容器的健康状态。RAGFlow 容器虽然设置了自动重启，但 Docker Desktop 未启动时容器不会运行。页面端口和 API 端口不同：页面使用 8088，平台后端调用 API 使用 9380。

## 文档解析提示 Embedding connection error

先访问 `http://localhost:8001/v1/models`，应返回 `BAAI/bge-m3`。随后确认 RAGFlow 中的 OpenAI 兼容向量模型地址是 `http://host.docker.internal:8001/v1`，模型名称和已解析知识库创建时选定的向量模型保持一致。模型服务只监听 127.0.0.1 时，容器可能无法访问，应确认监听地址与 Windows 防火墙规则。

## 平台同步失败

确认 Spring Boot 的本地配置包含 RAGFlow API Key 和 Chat Assistant ID。同步知识库使用 RAGFlow 的 `/api/v1/datasets` 接口；联合方案生成使用 Chat Assistant completion 接口，并在生成前调用 retrieval 接口取得所选知识库的真实证据。

## 验证标准

RAGFlow 能列出知识库与文档，BGE-M3 models 接口返回 200，平台模型配置页三个服务均显示 HEALTHY，知识检索能够返回带文档名和相似度的真实切片，联合方案任务最终状态为 SUCCEEDED 且存在引用。
