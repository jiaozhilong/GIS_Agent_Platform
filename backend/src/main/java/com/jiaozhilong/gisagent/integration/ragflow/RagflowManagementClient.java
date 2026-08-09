package com.jiaozhilong.gisagent.integration.ragflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.jiaozhilong.gisagent.common.exception.BusinessException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RagflowManagementClient {
    private final RagflowProperties properties;
    private final RestClient client;

    public RagflowManagementClient(RagflowProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.client = builder.baseUrl(properties.baseUrl()).build();
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
            result.add(new Document(text(item, "id", ""), text(item, "name", ""), item.path("chunk_count").asInt(0),
                    item.path("progress").asDouble(0), text(item, "run", "UNSTART"), text(item, "create_date", "")));
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
            JsonNode data = response.path("data");
            if (data.isArray()) for (JsonNode item : data) documentIds.add(text(item, "id", ""));
            if (!documentIds.isEmpty()) post("/api/v1/datasets/" + datasetId + "/chunks", Map.of("document_ids", documentIds));
            return documents(datasetId);
        } catch (RestClientException exception) {
            throw upstream("RAGFlow 文档上传或解析任务创建失败", exception);
        }
    }

    public RetrievalResult retrieve(String question, List<String> datasetIds, int topK, double threshold) {
        long started = System.nanoTime();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question", question);
        body.put("dataset_ids", datasetIds);
        body.put("page", 1);
        body.put("page_size", Math.max(1, Math.min(20, topK)));
        body.put("similarity_threshold", threshold);
        body.put("vector_similarity_weight", 0.5);
        body.put("top_k", 1024);
        JsonNode response = post("/api/v1/retrieval", body);
        List<RetrievalChunk> chunks = new ArrayList<>();
        for (JsonNode item : response.path("data").path("chunks")) {
            Map<String, String> metadata = new LinkedHashMap<>();
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
    public record Document(String id, String name, int chunkCount, double progress, String run, String createdAt) {}
    public record RetrievalChunk(String id, String datasetId, String documentId, String documentName, String content,
                                 double score, Integer pageNumber, Map<String, String> metadata) {}
    public record RetrievalResult(long durationMs, List<RetrievalChunk> chunks) {}
    public record ChatInfo(String id, String name, String modelName) {}
}
