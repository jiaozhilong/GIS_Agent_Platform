package com.jiaozhilong.gisagent.integration.ragflow;

import java.util.List;
import java.util.Map;

public interface RagflowGateway {
    RagflowGenerationResult generate(RagflowGenerationCommand command);

    record RagflowGenerationCommand(String assistantId, String sessionId, String userId, String prompt,
                                     List<String> knowledgeBaseIds, Map<String, Object> metadataCondition) {}
    record RagflowCitation(String chunkId, String datasetId, String documentId, String documentName,
                           String content, Double score, Integer pageNumber, Map<String, Object> metadata) {}
    record RagflowGenerationResult(String answer, String sessionId, String modelName, List<RagflowCitation> citations) {}
}
