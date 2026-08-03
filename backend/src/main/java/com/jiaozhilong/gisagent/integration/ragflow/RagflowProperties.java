package com.jiaozhilong.gisagent.integration.ragflow;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.ragflow")
public record RagflowProperties(String baseUrl, String apiKey, String defaultAssistantId, int timeoutSeconds) {
    public boolean configured() {
        return apiKey != null && !apiKey.isBlank() && defaultAssistantId != null && !defaultAssistantId.isBlank();
    }
}
