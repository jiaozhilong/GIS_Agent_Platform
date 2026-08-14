package com.jiaozhilong.gisagent.integration.ragflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.jiaozhilong.gisagent.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RagflowHttpGateway implements RagflowGateway {
    private final RagflowProperties properties;
    private final RestClient client;

    public RagflowHttpGateway(RagflowProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.client = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public RagflowGenerationResult generate(RagflowGenerationCommand command) {
        String assistantId = hasText(command.assistantId()) ? command.assistantId() : properties.defaultAssistantId();
        if (!hasText(properties.apiKey()) || !hasText(assistantId)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "UPSTREAM_ERROR", "RAGFlow API Key 或方案生成 Assistant ID 尚未配置");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question", command.prompt());
        body.put("stream", false);
        body.put("user_id", command.userId());
        if (hasText(command.sessionId())) body.put("session_id", command.sessionId());
        if (command.metadataCondition() != null && !command.metadataCondition().isEmpty()) body.put("metadata_condition", command.metadataCondition());
        try {
            JsonNode response = client.post()
                    .uri("/api/v1/chats/{assistantId}/completions", assistantId)
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) throw upstream("RAGFlow 返回空响应", null);
            JsonNode data = response.path("data");
            String answer = text(data, "answer", text(response, "answer", ""));
            String sessionId = text(data, "session_id", text(response, "session_id", command.sessionId()));
            String modelName = text(data, "model_name", text(data, "llm_name", null));
            return new RagflowGenerationResult(answer, sessionId, modelName, citations(data.path("reference").path("chunks")));
        } catch (RestClientException exception) {
            throw upstream("调用 RAGFlow 联合生成失败", exception);
        }
    }

    private List<RagflowCitation> citations(JsonNode chunks) {
        List<RagflowCitation> result = new ArrayList<>();
        if (!chunks.isArray()) return result;
        for (JsonNode chunk : chunks) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            JsonNode metadataNode = chunk.path("metadata");
            if (metadataNode.isObject()) metadataNode.fields().forEachRemaining(entry -> metadata.put(entry.getKey(), entry.getValue().isValueNode() ? entry.getValue().asText() : entry.getValue().toString()));
            result.add(new RagflowCitation(
                    text(chunk, "id", text(chunk, "chunk_id", null)),
                    text(chunk, "dataset_id", text(chunk, "kb_id", null)),
                    text(chunk, "document_id", text(chunk, "doc_id", null)),
                    text(chunk, "document_name", text(chunk, "docnm_kwd", null)),
                    text(chunk, "content", text(chunk, "content_with_weight", null)),
                    number(chunk, "similarity", number(chunk, "score", null)),
                    integer(chunk, "page_number", null), metadata));
        }
        return result;
    }

    private String text(JsonNode node, String field, String fallback) { JsonNode value = node.path(field); return value.isMissingNode() || value.isNull() ? fallback : value.asText(fallback); }
    private Double number(JsonNode node, String field, Double fallback) {
        JsonNode value = node.path(field);
        if (!value.isNumber()) return fallback;
        return Double.valueOf(value.asDouble());
    }

    private Integer integer(JsonNode node, String field, Integer fallback) {
        JsonNode value = node.path(field);
        if (!value.isNumber()) return fallback;
        return Integer.valueOf(value.asInt());
    }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private BusinessException upstream(String message, Exception cause) {
        BusinessException exception = new BusinessException(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR", message);
        if (cause != null) exception.initCause(cause);
        return exception;
    }
}
