package com.jiaozhilong.gisagent.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.jiaozhilong.gisagent.common.exception.BusinessException;
import com.jiaozhilong.gisagent.integration.ragflow.RagflowManagementClient;
import com.jiaozhilong.gisagent.integration.ragflow.RagflowProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ModelConfigService {
    public enum Provider { DEEPSEEK, RAGFLOW, BGE_M3, PLATFORM_LLM }
    public enum Category { KNOWLEDGE_ENGINE, PLATFORM_GENERATION }
    public enum Status { HEALTHY, UNAVAILABLE, UNCONFIGURED }
    public record Config(Provider provider, String displayName, Category category, String baseUrl, String modelName,
                         String maskedApiKey, Status status, OffsetDateTime lastCheckedAt, String message,
                         boolean editable, boolean enabled) {}
    public record UpdateRequest(String baseUrl, String modelName, String apiKey, Boolean enabled) {}
    public record PlatformGenerationResult(String answer, String modelName) {}
    private record PlatformRuntime(String baseUrl, String modelName, String apiKey, boolean enabled,
                                   Status lastTestStatus, String lastTestMessage, OffsetDateTime lastTestedAt) {}

    private final RagflowProperties ragflowProperties;
    private final RagflowManagementClient ragflow;
    private final RestClient.Builder restClientBuilder;
    private final JdbcTemplate jdbc;
    private final String embeddingBaseUrl;
    private final String defaultPlatformBaseUrl;
    private final String defaultPlatformModel;
    private final String defaultPlatformApiKey;
    private final boolean defaultPlatformEnabled;
    private final String encryptionSecret;

    public ModelConfigService(RagflowProperties ragflowProperties, RagflowManagementClient ragflow,
                              RestClient.Builder restClientBuilder, JdbcTemplate jdbc,
                              @Value("${integration.embedding.base-url:http://localhost:8001/v1}") String embeddingBaseUrl,
                              @Value("${platform.llm.base-url:}") String defaultPlatformBaseUrl,
                              @Value("${platform.llm.model-name:}") String defaultPlatformModel,
                              @Value("${platform.llm.api-key:}") String defaultPlatformApiKey,
                              @Value("${platform.llm.enabled:false}") boolean defaultPlatformEnabled,
                              @Value("${security.jwt.secret:}") String encryptionSecret) {
        this.ragflowProperties = ragflowProperties;
        this.ragflow = ragflow;
        this.restClientBuilder = restClientBuilder;
        this.jdbc = jdbc;
        this.embeddingBaseUrl = embeddingBaseUrl;
        this.defaultPlatformBaseUrl = defaultPlatformBaseUrl;
        this.defaultPlatformModel = defaultPlatformModel;
        this.defaultPlatformApiKey = defaultPlatformApiKey;
        this.defaultPlatformEnabled = defaultPlatformEnabled;
        this.encryptionSecret = encryptionSecret;
    }

    public List<Config> list() {
        return List.of(checkKnowledgeModel(Provider.RAGFLOW), checkKnowledgeModel(Provider.DEEPSEEK),
                checkKnowledgeModel(Provider.BGE_M3), platformSnapshot());
    }

    @Transactional
    public Config save(Provider provider, UpdateRequest request) {
        if (provider != Provider.PLATFORM_LLM) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "RAGFlow 内部模型由 RAGFlow 管理，请在 RAGFlow 或本地环境配置中修改");
        }
        boolean enabled = Boolean.TRUE.equals(request.enabled());
        String baseUrl = clean(request.baseUrl());
        String modelName = clean(request.modelName());
        PlatformRuntime current = platformRuntime();
        String apiKey = clean(request.apiKey());
        if (apiKey.isBlank() || apiKey.contains("****")) apiKey = current.apiKey();
        if (enabled && (baseUrl.isBlank() || modelName.isBlank() || apiKey.isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "启用平台生成模型前请填写服务地址、模型名称和 API Key");
        }
        jdbc.update("""
                insert into platform_model_configs(provider, base_url, model_name, api_key_ciphertext, enabled,
                  last_test_status, last_test_message, last_tested_at, updated_at)
                values ('PLATFORM_LLM', ?, ?, ?, ?, 'UNCONFIGURED', '配置已保存，尚未测试', null, current_timestamp)
                on conflict (provider) do update set base_url=excluded.base_url, model_name=excluded.model_name,
                  api_key_ciphertext=excluded.api_key_ciphertext, enabled=excluded.enabled,
                  last_test_status='UNCONFIGURED', last_test_message='配置已保存，尚未测试',
                  last_tested_at=null, updated_at=current_timestamp
                """, baseUrl, modelName, encrypt(apiKey), enabled);
        return platformSnapshot();
    }

    public Config test(Provider provider, UpdateRequest request) {
        if (provider == Provider.PLATFORM_LLM) return testPlatform(request);
        return checkKnowledgeModel(provider);
    }

    private Config checkKnowledgeModel(Provider provider) {
        OffsetDateTime checkedAt = OffsetDateTime.now();
        if (provider == Provider.BGE_M3) {
            try {
                restClientBuilder.build().get().uri(endpoint(embeddingBaseUrl, "models")).retrieve().toBodilessEntity();
                return config(provider, "BGE-M3 向量模型", embeddingBaseUrl, "BAAI/bge-m3", null,
                        Status.HEALTHY, checkedAt, "RAGFlow 文档向量化与召回服务可用", false, true);
            } catch (RuntimeException exception) {
                return config(provider, "BGE-M3 向量模型", embeddingBaseUrl, "BAAI/bge-m3", null,
                        Status.UNAVAILABLE, checkedAt, message(exception), false, true);
            }
        }
        if (provider == Provider.PLATFORM_LLM) return platformSnapshot();
        if (!ragflowProperties.configured()) {
            return config(provider, provider == Provider.RAGFLOW ? "RAGFlow 知识引擎" : "RAGFlow Assistant 语言模型",
                    ragflowProperties.baseUrl(), "", null, Status.UNCONFIGURED, checkedAt,
                    "RAGFlow API Key 或 Assistant ID 未配置", false, true);
        }
        try {
            if (provider == Provider.RAGFLOW) {
                int count = ragflow.datasets().size();
                return config(provider, "RAGFlow 知识引擎", ragflowProperties.baseUrl(), "RAGFlow v0.26.4",
                        mask(ragflowProperties.apiKey()), Status.HEALTHY, checkedAt, "已连接 " + count + " 个知识库", false, true);
            }
            RagflowManagementClient.ChatInfo assistant = ragflow.assistant();
            if (assistant == null) return config(provider, "RAGFlow Assistant 语言模型", ragflowProperties.baseUrl(), "",
                    null, Status.UNAVAILABLE, checkedAt, "目标 Assistant 不存在", false, true);
            return config(provider, "RAGFlow Assistant 语言模型", ragflowProperties.baseUrl(), assistant.modelName(),
                    null, Status.HEALTHY, checkedAt, "该模型供 RAGFlow 内部问答与回退生成使用", false, true);
        } catch (RuntimeException exception) {
            return config(provider, provider == Provider.RAGFLOW ? "RAGFlow 知识引擎" : "RAGFlow Assistant 语言模型",
                    ragflowProperties.baseUrl(), "", null, Status.UNAVAILABLE, checkedAt, message(exception), false, true);
        }
    }

    public boolean platformGenerationEnabled() { return platformRuntime().enabled(); }

    public PlatformGenerationResult generateWithPlatformModel(String prompt) {
        PlatformRuntime runtime = platformRuntime();
        if (!runtime.enabled() || runtime.baseUrl().isBlank() || runtime.modelName().isBlank() || runtime.apiKey().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR", "平台方案生成模型尚未完整配置");
        }
        try {
            JsonNode response = restClientBuilder.build().post().uri(endpoint(runtime.baseUrl(), "chat/completions"))
                    .header("Authorization", "Bearer " + runtime.apiKey())
                    .body(Map.of("model", runtime.modelName(), "messages", List.of(
                            Map.of("role", "system", "content", "你是严谨的 GIS 行业解决方案编制智能体。"),
                            Map.of("role", "user", "content", prompt))))
                    .retrieve().body(JsonNode.class);
            String answer = response == null ? "" : response.path("choices").path(0).path("message").path("content").asText("");
            if (answer.isBlank()) throw new IllegalStateException("模型返回内容为空");
            String model = response.path("model").asText(runtime.modelName());
            return new PlatformGenerationResult(answer, model);
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR", "平台方案生成模型调用失败：" + message(exception));
        }
    }

    private Config platformSnapshot() {
        PlatformRuntime runtime = platformRuntime();
        String message = runtime.lastTestMessage();
        if (message.isBlank()) {
            message = runtime.enabled() ? "配置已启用，建议执行连接测试" : "未启用，方案生成将回退到 RAGFlow Assistant";
        }
        return config(Provider.PLATFORM_LLM, "平台方案生成模型", runtime.baseUrl(), runtime.modelName(),
                mask(runtime.apiKey()), runtime.lastTestStatus(), runtime.lastTestedAt(), message, true, runtime.enabled());
    }

    private Config testPlatform(UpdateRequest request) {
        PlatformRuntime stored = platformRuntime();
        String baseUrl = request == null ? stored.baseUrl() : clean(request.baseUrl());
        String modelName = request == null ? stored.modelName() : clean(request.modelName());
        String apiKey = request == null ? stored.apiKey() : clean(request.apiKey());
        if (apiKey.isBlank() || apiKey.contains("****")) apiKey = stored.apiKey();
        boolean enabled = request == null ? stored.enabled() : Boolean.TRUE.equals(request.enabled());
        OffsetDateTime checkedAt = OffsetDateTime.now();
        if (baseUrl.isBlank() || modelName.isBlank() || apiKey.isBlank()) {
            return config(Provider.PLATFORM_LLM, "平台方案生成模型", baseUrl, modelName, mask(apiKey),
                    Status.UNCONFIGURED, checkedAt, "请先填写服务地址、模型名称和 API Key", true, enabled);
        }
        Status status;
        String testMessage;
        try {
            JsonNode response = restClientBuilder.build().post().uri(endpoint(baseUrl, "chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of("model", modelName, "messages", List.of(
                            Map.of("role", "user", "content", "请仅回复 OK，用于连接测试。"))))
                    .retrieve().body(JsonNode.class);
            if (response == null || !response.path("choices").isArray() || response.path("choices").isEmpty()) {
                throw new IllegalStateException("服务响应中没有 choices，可能不是 OpenAI Chat Completions 兼容接口");
            }
            status = Status.HEALTHY;
            testMessage = "连接成功，API Key 与模型名称均可用";
        } catch (RuntimeException exception) {
            status = Status.UNAVAILABLE;
            testMessage = "连接失败：" + message(exception);
        }
        if (sameConfig(stored, baseUrl, modelName, apiKey)) {
            jdbc.update("update platform_model_configs set last_test_status=?, last_test_message=?, last_tested_at=? where provider='PLATFORM_LLM'",
                    status.name(), testMessage, checkedAt);
        }
        return config(Provider.PLATFORM_LLM, "平台方案生成模型", baseUrl, modelName, mask(apiKey),
                status, checkedAt, testMessage, true, enabled);
    }

    private PlatformRuntime platformRuntime() {
        var rows = jdbc.query("select base_url, model_name, api_key_ciphertext, enabled, last_test_status, last_test_message, last_tested_at from platform_model_configs where provider='PLATFORM_LLM'",
                (rs, rowNum) -> {
                    String ciphertext = rs.getString("api_key_ciphertext");
                    String apiKey = decrypt(ciphertext);
                    boolean credentialInvalid = ciphertext != null && !ciphertext.isBlank() && apiKey.isBlank();
                    return new PlatformRuntime(rs.getString("base_url"), rs.getString("model_name"), apiKey,
                            rs.getBoolean("enabled"),
                            credentialInvalid ? Status.UNAVAILABLE : Status.valueOf(rs.getString("last_test_status")),
                            credentialInvalid ? "已保存的 API Key 无法解密，请重新输入并保存" : rs.getString("last_test_message"),
                            rs.getObject("last_tested_at", OffsetDateTime.class));
                });
        if (!rows.isEmpty()) {
            PlatformRuntime stored = rows.get(0);
            boolean completelyEmpty = stored.baseUrl().isBlank() && stored.modelName().isBlank() && stored.apiKey().isBlank();
            if (!completelyEmpty) return stored;
        }
        return new PlatformRuntime(clean(defaultPlatformBaseUrl), clean(defaultPlatformModel), clean(defaultPlatformApiKey),
                defaultPlatformEnabled, Status.UNCONFIGURED, "", null);
    }

    private boolean sameConfig(PlatformRuntime stored, String baseUrl, String modelName, String apiKey) {
        return stored.baseUrl().equals(baseUrl) && stored.modelName().equals(modelName) && stored.apiKey().equals(apiKey);
    }

    private Config config(Provider provider, String displayName, String baseUrl, String modelName, String maskedApiKey,
                          Status status, OffsetDateTime checkedAt, String message, boolean editable, boolean enabled) {
        return new Config(provider, displayName, provider == Provider.PLATFORM_LLM ? Category.PLATFORM_GENERATION : Category.KNOWLEDGE_ENGINE,
                baseUrl, modelName, maskedApiKey, status, checkedAt, message, editable, enabled);
    }

    private String encrypt(String value) {
        if (value == null || value.isBlank()) return "";
        if (encryptionSecret == null || encryptionSecret.isBlank()) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "JWT 密钥未配置，无法安全保存模型 API Key");
        }
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception exception) { throw new IllegalStateException("模型 API Key 加密失败", exception); }
    }

    private String decrypt(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            byte[] payload = Base64.getDecoder().decode(value);
            byte[] iv = java.util.Arrays.copyOfRange(payload, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, 12, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            // A changed local secret must not make the entire model settings page unavailable.
            // The UI will ask the administrator to enter and persist the credential again.
            return "";
        }
    }

    private SecretKeySpec key() throws Exception {
        return new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(encryptionSecret.getBytes(StandardCharsets.UTF_8)), "AES");
    }

    private String endpoint(String baseUrl, String path) { return clean(baseUrl).replaceAll("/+$", "") + "/" + path; }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String mask(String value) { return value == null || value.isBlank() ? null : value.length() < 8 ? "****" : value.substring(0, 4) + "****" + value.substring(value.length() - 4); }
    private String message(RuntimeException exception) { return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(); }
}
