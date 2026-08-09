package com.jiaozhilong.gisagent.settings;

import com.jiaozhilong.gisagent.integration.ragflow.RagflowManagementClient;
import com.jiaozhilong.gisagent.integration.ragflow.RagflowProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ModelConfigService {
    public enum Provider { DEEPSEEK, RAGFLOW, BGE_M3 }
    public enum Status { HEALTHY, UNAVAILABLE, UNCONFIGURED }
    public record Config(Provider provider, String displayName, String baseUrl, String modelName,
                         String maskedApiKey, Status status, OffsetDateTime lastCheckedAt, String message) {}
    public record UpdateRequest(String baseUrl, String modelName) {}

    private final RagflowProperties ragflowProperties;
    private final RagflowManagementClient ragflow;
    private final RestClient.Builder restClientBuilder;
    private final Map<Provider, UpdateRequest> overrides = new EnumMap<>(Provider.class);
    private final String embeddingBaseUrl;

    public ModelConfigService(RagflowProperties ragflowProperties, RagflowManagementClient ragflow,
                              RestClient.Builder restClientBuilder,
                              @Value("${integration.embedding.base-url:http://localhost:8001/v1}") String embeddingBaseUrl) {
        this.ragflowProperties = ragflowProperties;
        this.ragflow = ragflow;
        this.restClientBuilder = restClientBuilder;
        this.embeddingBaseUrl = embeddingBaseUrl;
    }

    public List<Config> list() { return List.of(check(Provider.RAGFLOW), check(Provider.DEEPSEEK), check(Provider.BGE_M3)); }

    public Config save(Provider provider, UpdateRequest request) {
        overrides.put(provider, new UpdateRequest(clean(request.baseUrl()), clean(request.modelName())));
        return check(provider);
    }

    public Config check(Provider provider) {
        OffsetDateTime checkedAt = OffsetDateTime.now();
        UpdateRequest override = overrides.get(provider);
        if (provider == Provider.BGE_M3) {
            String baseUrl = override != null && !override.baseUrl().isBlank() ? override.baseUrl() : embeddingBaseUrl;
            String model = override != null && !override.modelName().isBlank() ? override.modelName() : "BAAI/bge-m3";
            try {
                restClientBuilder.baseUrl(baseUrl).build().get().uri("/models").retrieve().toBodilessEntity();
                return new Config(provider, "BGE-M3 向量模型", baseUrl, model, null, Status.HEALTHY, checkedAt, "本地向量服务可用");
            } catch (RuntimeException exception) {
                return new Config(provider, "BGE-M3 向量模型", baseUrl, model, null, Status.UNAVAILABLE, checkedAt, exception.getMessage());
            }
        }
        String baseUrl = override != null && !override.baseUrl().isBlank() ? override.baseUrl() : ragflowProperties.baseUrl();
        if (!ragflowProperties.configured()) {
            return new Config(provider, provider == Provider.RAGFLOW ? "RAGFlow 知识中台" : "DeepSeek 生成模型", baseUrl, "",
                    null, Status.UNCONFIGURED, checkedAt, "RAGFlow API Key 或 Assistant ID 未配置");
        }
        try {
            if (provider == Provider.RAGFLOW) {
                int count = ragflow.datasets().size();
                return new Config(provider, "RAGFlow 知识中台", baseUrl, "RAGFlow v0.26.4", mask(ragflowProperties.apiKey()),
                        Status.HEALTHY, checkedAt, "已连接 " + count + " 个知识库");
            }
            RagflowManagementClient.ChatInfo assistant = ragflow.assistant();
            if (assistant == null) return new Config(provider, "DeepSeek 生成模型", baseUrl, "", null, Status.UNAVAILABLE, checkedAt, "目标 Assistant 不存在");
            String model = override != null && !override.modelName().isBlank() ? override.modelName() : assistant.modelName();
            Status status = model.toLowerCase().contains("deepseek") ? Status.HEALTHY : Status.UNAVAILABLE;
            return new Config(provider, "DeepSeek 生成模型", baseUrl, model, null, status, checkedAt,
                    status == Status.HEALTHY ? "Assistant 已绑定 DeepSeek" : "Assistant 未绑定 DeepSeek");
        } catch (RuntimeException exception) {
            return new Config(provider, provider == Provider.RAGFLOW ? "RAGFlow 知识中台" : "DeepSeek 生成模型", baseUrl, "",
                    null, Status.UNAVAILABLE, checkedAt, exception.getMessage());
        }
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String mask(String value) { return value == null || value.length() < 8 ? "****" : value.substring(0, 8) + "****" + value.substring(value.length() - 4); }
}
