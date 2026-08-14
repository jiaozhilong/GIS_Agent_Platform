package com.jiaozhilong.gisagent.knowledge;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RetrievalEvaluationDtos {
    private RetrievalEvaluationDtos() {}
    public record Case(UUID id, String query, String expectedDatasetKeyword, String expectedDocumentKeyword,
                       String expectedSection, Integer expectedPage, Integer expectedSlide,
                       String expectedKnowledgeType, boolean active) {}
    public record Run(UUID id, String status, int totalCases, int completedCases, Map<String, Object> metrics,
                      String errorMessage, OffsetDateTime startedAt, OffsetDateTime finishedAt,
                      OffsetDateTime createdAt, List<Result> results) {}
    public record Result(UUID caseId, String query, long durationMs, Integer datasetHitRank,
                         Integer documentHitRank, Integer slideHitRank, String errorMessage) {}
}
