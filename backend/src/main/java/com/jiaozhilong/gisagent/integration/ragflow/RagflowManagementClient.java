package com.jiaozhilong.gisagent.integration.ragflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.jiaozhilong.gisagent.common.exception.BusinessException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import java.time.Duration;

@Component
public class RagflowManagementClient {
    private final RagflowProperties properties;
    private final RestClient client;

    public RagflowManagementClient(RagflowProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        int timeoutMillis = Math.max(30, properties.timeoutSeconds()) * 1000;
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.min(timeoutMillis, 30_000)))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMillis));
        this.client = builder.clone().requestFactory(requestFactory).baseUrl(properties.baseUrl()).build();
    }

    public List<Dataset> datasets() {
        JsonNode response = get("/api/v1/datasets?page=1&page_size=100");
        List<Dataset> result = new ArrayList<>();
        for (JsonNode item : response.path("data")) {
            result.add(new Dataset(
                    text(item, "id", ""), text(item, "name", ""), text(item, "description", ""),
                    item.path("document_count").asInt(0), item.path("chunk_count").asInt(0),
                    text(item, "embedding_model", ""), text(item, "chunk_method", "manual"),
                    "1".equals(text(item, "status", "1")), text(item, "update_date", text(item, "create_date", ""))));
        }
        return result;
    }

    public List<Document> documents(String datasetId) {
        JsonNode response = get("/api/v1/datasets/" + datasetId + "/documents?page=1&page_size=100");
        JsonNode data = response.path("data");
        JsonNode docs = data.isArray() ? data : data.path("docs");
        List<Document> result = new ArrayList<>();
        if (!docs.isArray()) return result;
        for (JsonNode item : docs) {
            result.add(document(item));
        }
        return result;
    }

    public Dataset createDataset(String name, String description, String chunkMethod) {
        JsonNode response = post("/api/v1/datasets", Map.of(
                "name", name,
                "description", description == null ? "" : description,
                "chunk_method", chunkMethod == null || chunkMethod.isBlank() ? "manual" : chunkMethod));
        JsonNode item = response.path("data");
        return new Dataset(text(item, "id", ""), text(item, "name", name), text(item, "description", description),
                0, 0, text(item, "embedding_model", ""), text(item, "chunk_method", chunkMethod), true,
                text(item, "update_date", ""));
    }

    public List<Document> uploadAndParse(String datasetId, String filename, byte[] content) {
        uploadAndParseDocument(datasetId, filename, content);
        return documents(datasetId);
    }

    public UploadedDocument uploadAndParseDocument(String datasetId, String filename, byte[] content) {
        UploadedDocument uploaded = uploadDocument(datasetId, filename, content);
        post("/api/v1/datasets/" + datasetId + "/chunks", Map.of("document_ids", List.of(uploaded.id())));
        return uploaded;
    }

    public UploadedDocument uploadDocument(String datasetId, String filename, byte[] content) {
        requireConfigured();
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", new ByteArrayResource(content) {
            @Override public String getFilename() { return filename; }
        });
        try {
            JsonNode response = client.post().uri("/api/v1/datasets/{datasetId}/documents", datasetId)
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart).retrieve().body(JsonNode.class);
            ensureSuccess(response);
            List<String> documentIds = new ArrayList<>();
            String uploadedName = filename;
            JsonNode data = response.path("data");
            if (data.isArray()) for (JsonNode item : data) {
                documentIds.add(text(item, "id", ""));
                uploadedName = text(item, "name", uploadedName);
            }
            if (documentIds.isEmpty()) throw upstream("RAGFlow 上传成功但未返回文档 ID", null);
            return new UploadedDocument(documentIds.get(0), uploadedName);
        } catch (RestClientException exception) {
            throw upstream("RAGFlow 文档上传失败", exception);
        }
    }

    public UploadedDocument uploadDocument(String datasetId, String filename, Path path) {
        requireConfigured();
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", new FileSystemResource(path) {
            @Override public String getFilename() { return filename; }
        });
        long started = System.nanoTime();
        try {
            JsonNode response = client.post().uri("/api/v1/datasets/{datasetId}/documents", datasetId)
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart).retrieve().body(JsonNode.class);
            ensureSuccess(response);
            JsonNode data = response.path("data");
            JsonNode item = data.isArray() && !data.isEmpty() ? data.get(0) : data;
            String id = text(item, "id", "");
            if (id.isBlank()) throw upstream("RAGFlow 上传成功但未返回文档 ID", null);
            logCall("uploadDocument", datasetId, id, started, "SUCCEEDED", null);
            return new UploadedDocument(id, text(item, "name", filename));
        } catch (RestClientException exception) {
            logCall("uploadDocument", datasetId, null, started, "FAILED", exception.getMessage());
            throw upstream("RAGFlow 文档上传失败", exception);
        }
    }

    public Document updateDocument(String datasetId, String documentId, String chunkMethod,
                                   Map<String, Object> parserConfig, Map<String, Object> metadata) {
        Map<String, Object> body = documentUpdateBody(chunkMethod, parserConfig, metadata);
        JsonNode response = patch("/api/v1/datasets/" + datasetId + "/documents/" + documentId, body);
        return document(response.path("data"));
    }

    static Map<String, Object> documentUpdateBody(String chunkMethod, Map<String, Object> parserConfig,
                                                   Map<String, Object> metadata) {
        Map<String, Object> body = new LinkedHashMap<>();
        // RAGFlow v0.26.x assigns presentation/picture from the file type during upload and explicitly
        // rejects changing chunk_method for PPT/PPTX/visual documents with "Not supported yet!".
        // Parser config and metadata are still document-scoped and can be patched normally.
        if (chunkMethod != null && !chunkMethod.isBlank()
                && !"presentation".equalsIgnoreCase(chunkMethod)
                && !"picture".equalsIgnoreCase(chunkMethod)) body.put("chunk_method", chunkMethod);
        if (parserConfig != null && !parserConfig.isEmpty()) body.put("parser_config", parserConfig);
        if (metadata != null) body.put("meta_fields", metadata);
        return body;
    }

    public void parseDocument(String datasetId, String documentId) {
        post("/api/v1/datasets/" + datasetId + "/chunks", Map.of("document_ids", List.of(documentId)));
    }

    public Document getDocument(String datasetId, String documentId) {
        JsonNode response = get("/api/v1/datasets/" + datasetId + "/documents?page=1&page_size=20&id=" + documentId);
        JsonNode data = response.path("data");
        JsonNode docs = data.isArray() ? data : data.path("docs");
        if (docs.isArray()) for (JsonNode item : docs) if (documentId.equals(text(item, "id", ""))) return document(item);
        return null;
    }

    public void deleteAllChunks(String datasetId, String documentId) {
        delete("/api/v1/datasets/" + datasetId + "/documents/" + documentId + "/chunks", Map.of("delete_all", true));
    }

    public void deleteDocument(String datasetId, String documentId) {
        delete("/api/v1/datasets/" + datasetId + "/documents", Map.of("ids", List.of(documentId), "delete_all", false));
    }

    public String addChunk(String datasetId, String documentId, String content) {
        if (content == null || content.isBlank()) throw upstream("RAGFlow 切片内容不能为空", null);
        JsonNode response = post("/api/v1/datasets/" + datasetId + "/documents/" + documentId + "/chunks",
                Map.of("content", content));
        JsonNode chunk = response.path("data").path("chunk");
        String chunkId = text(chunk, "id", "");
        if (chunkId.isBlank()) throw upstream("RAGFlow 新增切片成功但未返回切片 ID", null);
        return chunkId;
    }

    public RetrievalResult retrieve(String question, List<String> datasetIds, int topK, double threshold) {
        return retrieve(new RetrievalRequest(question, datasetIds, List.of(), 1, topK, threshold, .3,
                1024, true, Map.of()));
    }

    public RetrievalResult retrieve(RetrievalRequest request) {
        long started = System.nanoTime();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question", request.question());
        body.put("dataset_ids", request.datasetIds());
        if (request.documentIds() != null && !request.documentIds().isEmpty()) body.put("document_ids", request.documentIds());
        body.put("page", request.page());
        body.put("page_size", Math.max(1, Math.min(100, request.pageSize())));
        body.put("similarity_threshold", request.similarityThreshold());
        body.put("vector_similarity_weight", request.vectorSimilarityWeight());
        body.put("top_k", request.topK());
        body.put("keyword", request.keyword());
        if (request.metadataCondition() != null && !request.metadataCondition().isEmpty()) body.put("metadata_condition", request.metadataCondition());
        JsonNode response;
        try {
            response = post("/api/v1/retrieval", body);
        } catch (BusinessException exception) {
            if (!request.keyword()) throw exception;
            body.put("keyword", false);
            response = post("/api/v1/retrieval", body);
        }
        List<RetrievalChunk> chunks = new ArrayList<>();
        for (JsonNode item : response.path("data").path("chunks")) {
            Map<String, String> metadata = new LinkedHashMap<>();
            JsonNode metadataNode = item.path("metadata");
            if (!metadataNode.isObject()) metadataNode = item.path("meta_fields");
            if (metadataNode.isObject()) metadataNode.fields().forEachRemaining(entry -> metadata.put(entry.getKey(), entry.getValue().asText()));
            JsonNode positions = item.path("positions");
            Integer pageNumber = null;
            if (positions.isArray() && !positions.isEmpty() && positions.get(0).isArray() && !positions.get(0).isEmpty()) {
                pageNumber = positions.get(0).get(0).asInt() + 1;
            }
            chunks.add(new RetrievalChunk(text(item, "id", ""), text(item, "dataset_id", ""),
                    text(item, "document_id", ""), text(item, "document_keyword", ""),
                    text(item, "content", ""), item.path("similarity").asDouble(0), pageNumber, metadata));
        }
        return new RetrievalResult((System.nanoTime() - started) / 1_000_000, chunks);
    }

    private Document document(JsonNode item) {
        return new Document(text(item, "id", ""), text(item, "name", ""), item.path("chunk_count").asInt(0),
                item.path("progress").asDouble(0), text(item, "run", "UNSTART"),
                text(item, "progress_msg", ""), text(item, "create_date", ""));
    }

    public ChatInfo assistant() {
        JsonNode response = get("/api/v1/chats?page=1&page_size=100");
        JsonNode chats = response.path("data").path("chats");
        for (JsonNode item : chats) {
            if (properties.defaultAssistantId().equals(text(item, "id", ""))) {
                return new ChatInfo(text(item, "id", ""), text(item, "name", ""), text(item, "llm_id", ""));
            }
        }
        return null;
    }

    private JsonNode get(String path) {
        requireConfigured();
        try {
            JsonNode response = client.get().uri(path).header("Authorization", "Bearer " + properties.apiKey())
                    .retrieve().body(JsonNode.class);
            return ensureSuccess(response);
        } catch (RestClientException exception) {
            throw upstream("连接 RAGFlow 管理接口失败", exception);
        }
    }

    private JsonNode post(String path, Object body) {
        requireConfigured();
        try {
            JsonNode response = client.post().uri(path).header("Authorization", "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
            return ensureSuccess(response);
        } catch (RestClientException exception) {
            throw upstream("调用 RAGFlow 接口失败", exception);
        }
    }

    private JsonNode patch(String path, Object body) {
        requireConfigured();
        try {
            JsonNode response = client.patch().uri(path).header("Authorization", "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
            return ensureSuccess(response);
        } catch (RestClientException exception) {
            throw upstream("调用 RAGFlow 文档配置接口失败", exception);
        }
    }

    private JsonNode delete(String path, Object body) {
        requireConfigured();
        try {
            JsonNode response = client.method(org.springframework.http.HttpMethod.DELETE).uri(path)
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
            return ensureSuccess(response);
        } catch (RestClientException exception) {
            throw upstream("调用 RAGFlow 删除切片接口失败", exception);
        }
    }

    private void logCall(String endpoint, String datasetId, String documentId, long started, String status, String error) {
        long duration = (System.nanoTime() - started) / 1_000_000;
        org.slf4j.LoggerFactory.getLogger(RagflowManagementClient.class).info(
                "RAGFlow endpoint={} datasetId={} documentId={} durationMs={} status={} error={}",
                endpoint, datasetId, documentId, duration, status, error == null ? "" : error);
    }

    private JsonNode ensureSuccess(JsonNode response) {
        if (response == null) throw upstream("RAGFlow 返回空响应", null);
        if (response.path("code").asInt(-1) != 0) {
            throw upstream("RAGFlow 返回错误：" + text(response, "message", "unknown error"), null);
        }
        return response;
    }

    private void requireConfigured() {
        if (!properties.configured()) throw upstream("RAGFlow API Key 或 Assistant ID 尚未配置", null);
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? fallback : value.asText(fallback);
    }

    private BusinessException upstream(String message, Exception cause) {
        return new BusinessException(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR",
                cause == null || cause.getMessage() == null ? message : message + "：" + cause.getMessage());
    }

    public record Dataset(String id, String name, String description, int documentCount, int chunkCount,
                          String embeddingModel, String chunkMethod, boolean ready, String updatedAt) {}
    public record Document(String id, String name, int chunkCount, double progress, String run,
                           String progressMessage, String createdAt) {}
    public record UploadedDocument(String id, String name) {}
    public record RetrievalChunk(String id, String datasetId, String documentId, String documentName, String content,
                                 double score, Integer pageNumber, Map<String, String> metadata) {}
    public record RetrievalResult(long durationMs, List<RetrievalChunk> chunks) {}
    public record RetrievalRequest(String question, List<String> datasetIds, List<String> documentIds, int page,
                                   int pageSize, double similarityThreshold, double vectorSimilarityWeight,
                                   int topK, boolean keyword, Map<String, Object> metadataCondition) {}
    public record ChatInfo(String id, String name, String modelName) {}
}
