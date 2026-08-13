package com.jiaozhilong.gisagent.integration.ragflow;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RagflowManagementClientTest {
    @Test
    void omitsChunkMethodForPptxBecauseRagflowAssignsPresentationOnUpload() {
        Map<String, Object> body = RagflowManagementClient.documentUpdateBody(
                "presentation", Map.of("chunk_token_num", 256), Map.of("asset_id", "asset-1"));
        assertThat(body).doesNotContainKey("chunk_method");
        assertThat(body).containsEntry("parser_config", Map.of("chunk_token_num", 256));
        assertThat(body).containsEntry("meta_fields", Map.of("asset_id", "asset-1"));
    }

    @Test
    void keepsDocumentLevelChunkMethodForGeneralDocuments() {
        Map<String, Object> body = RagflowManagementClient.documentUpdateBody("naive", Map.of(), Map.of());
        assertThat(body).containsEntry("chunk_method", "naive");
        assertThat(body).containsKey("meta_fields");
    }
}
